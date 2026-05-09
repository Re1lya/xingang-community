package com.xingang.community.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingang.community.common.constant.RedisConstants;
import com.xingang.community.entity.Shop;
import com.xingang.community.entity.ShopType;
import com.xingang.community.shop.mapper.ShopMapper;
import com.xingang.community.shop.mapper.ShopTypeMapper;
import com.xingang.community.shop.service.ShopService;
import com.xingang.community.vo.ShopTypeVO;
import com.xingang.community.vo.ShopVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商户服务，含Redis缓存、缓存空值防穿透、互斥锁防击穿、Redis GEO附近查询。
 */
@Slf4j
@Service
public class ShopServiceImpl implements ShopService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private ShopTypeMapper shopTypeMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ShopVO queryById(Long id) {
        String cacheKey = RedisConstants.CACHE_SHOP_KEY + id;

        // 1. 查Redis缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if ("".equals(cached)) {
                return null; // 空值缓存，防穿透
            }
            try {
                return MAPPER.readValue(cached, ShopVO.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize shop cache: id={}", id, e);
            }
        }

        // 2. 缓存未命中，使用互斥锁防击穿
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        try {
            boolean locked = tryLock(lockKey);
            if (!locked) {
                // 获取锁失败，短暂等待后重试
                Thread.sleep(50);
                return queryById(id); // 递归重试
            }

            // 双重检查
            cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if ("".equals(cached)) return null;
                try {
                    return MAPPER.readValue(cached, ShopVO.class);
                } catch (JsonProcessingException ignored) {}
            }

            // 3. 查MySQL
            Shop shop = shopMapper.selectById(id);
            if (shop == null) {
                // 写入空值缓存防穿透
                stringRedisTemplate.opsForValue().set(
                        cacheKey, "",
                        RedisConstants.CACHE_NULL_TTL_MINUTES, TimeUnit.MINUTES
                );
                return null;
            }

            ShopVO vo = toVO(shop);
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    MAPPER.writeValueAsString(vo),
                    RedisConstants.CACHE_SHOP_TTL_MINUTES, TimeUnit.MINUTES
            );
            return vo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化商户数据失败", e);
        } finally {
            unlock(lockKey);
        }
    }

    @Override
    public List<ShopTypeVO> queryShopTypeList() {
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;

        // 查缓存
        List<String> cachedList = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (!CollectionUtils.isEmpty(cachedList)) {
            return cachedList.stream().map(s -> {
                try {
                    return MAPPER.readValue(s, ShopTypeVO.class);
                } catch (JsonProcessingException e) {
                    return null;
                }
            }).filter(v -> v != null).collect(Collectors.toList());
        }

        // 查MySQL
        List<ShopType> types = shopTypeMapper.selectList(
                new LambdaQueryWrapper<ShopType>().orderByAsc(ShopType::getSort)
        );
        List<ShopTypeVO> vos = types.stream().map(this::toTypeVO).collect(Collectors.toList());

        // 写缓存
        vos.forEach(vo -> {
            try {
                stringRedisTemplate.opsForList().rightPush(key, MAPPER.writeValueAsString(vo));
            } catch (JsonProcessingException ignored) {}
        });

        return vos;
    }

    @Override
    public List<ShopVO> queryShopByType(Long typeId, Integer current, Integer pageSize, Double x, Double y) {
        // 有经纬度 → Redis GEO附近查询
        if (x != null && y != null) {
            return queryNearbyShops(typeId, x, y, current, pageSize);
        }

        // 无经纬度 → MySQL分页查询
        Page<Shop> page = shopMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<Shop>().eq(Shop::getTypeId, typeId)
        );
        return page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 基于Redis GEO按分类查询附近门店。
     */
    private List<ShopVO> queryNearbyShops(Long typeId, Double x, Double y, Integer current, Integer pageSize) {
        String geoKey = RedisConstants.SHOP_GEO_KEY + typeId;

        // GEO按距离查询
        Point point = new Point(x, y);
        Distance distance = new Distance(10, Metrics.KILOMETERS);
        Circle circle = new Circle(point, distance);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(geoKey, circle);

        if (results == null || CollectionUtils.isEmpty(results.getContent())) {
            return Collections.emptyList();
        }

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();

        // 手动分页
        int fromIndex = (current - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, content.size());
        if (fromIndex >= content.size()) {
            return Collections.emptyList();
        }

        List<ShopVO> vos = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            GeoResult<RedisGeoCommands.GeoLocation<String>> geo = content.get(i);
            String member = geo.getContent().getName(); // shopId
            Long shopId = Long.valueOf(member);
            Shop shop = shopMapper.selectById(shopId);
            if (shop != null) {
                ShopVO vo = toVO(shop);
                vo.setDistance(geo.getDistance().getValue());
                vos.add(vo);
            }
        }
        return vos;
    }

    // ---- 互斥锁工具 ----

    private boolean tryLock(String key) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    // ---- VO转换 ----

    private ShopVO toVO(Shop shop) {
        ShopVO vo = new ShopVO();
        vo.setId(shop.getId());
        vo.setName(shop.getName());
        vo.setTypeId(shop.getTypeId());
        vo.setArea(shop.getArea());
        vo.setAddress(shop.getAddress());
        vo.setX(shop.getX());
        vo.setY(shop.getY());
        vo.setAvgPrice(shop.getAvgPrice());
        vo.setScore(shop.getScore());
        vo.setOpenHours(shop.getOpenHours());
        vo.setComments(shop.getComments());
        return vo;
    }

    private ShopTypeVO toTypeVO(ShopType type) {
        ShopTypeVO vo = new ShopTypeVO();
        vo.setId(type.getId());
        vo.setName(type.getName());
        vo.setIcon(type.getIcon());
        vo.setSort(type.getSort());
        return vo;
    }
}
