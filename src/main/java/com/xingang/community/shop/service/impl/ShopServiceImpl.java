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
import java.util.UUID;
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

    private static final int MAX_RETRY = 3;
    private static final long RETRY_SLEEP_MS = 50;

    @Override
    public ShopVO queryById(Long id) {
        String cacheKey = RedisConstants.CACHE_SHOP_KEY + id;

        // 1. 查Redis缓存
        ShopVO cachedVO = getCachedShop(cacheKey);
        if (cachedVO != null || isNullCached(cacheKey)) {
            return cachedVO; // 命中缓存或空值缓存
        }

        // 2. 缓存未命中，使用互斥锁防击穿（有限重试）
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            String lockToken = tryLockWithToken(lockKey);
            if (lockToken != null) {
                try {
                    // 双重检查
                    cachedVO = getCachedShop(cacheKey);
                    if (cachedVO != null || isNullCached(cacheKey)) {
                        return cachedVO;
                    }
                    // 查MySQL并写缓存
                    return loadShopAndCache(id, cacheKey);
                } finally {
                    unlockWithToken(lockKey, lockToken);
                }
            }
            // 未获取锁，等待后重试
            sleepUninterruptedly(RETRY_SLEEP_MS);
        }

        // 超过最大重试次数，直接查MySQL（降级）
        log.warn("Failed to acquire lock after {} retries, fallback to DB: shopId={}", MAX_RETRY, id);
        return loadShopAndCache(id, cacheKey);
    }

    private ShopVO getCachedShop(String cacheKey) {
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached == null || "".equals(cached)) {
            return null;
        }
        try {
            return MAPPER.readValue(cached, ShopVO.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize shop cache: key={}", cacheKey, e);
            return null;
        }
    }

    private boolean isNullCached(String cacheKey) {
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        return "".equals(cached);
    }

    private ShopVO loadShopAndCache(Long id, String cacheKey) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(
                    cacheKey, "",
                    RedisConstants.CACHE_NULL_TTL_MINUTES, TimeUnit.MINUTES
            );
            return null;
        }
        ShopVO vo = toVO(shop);
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    MAPPER.writeValueAsString(vo),
                    RedisConstants.CACHE_SHOP_TTL_MINUTES, TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize shop: id={}", id, e);
        }
        return vo;
    }

    private void sleepUninterruptedly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    // ---- 互斥锁工具（基于唯一token校验释放） ----

    /**
     * 尝试获取锁，成功返回唯一token，失败返回null。
     * 只有持有该token的线程才能释放锁，防止误删。
     */
    private String tryLockWithToken(String key) {
        String token = UUID.randomUUID().toString();
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, RedisConstants.LOCK_SHOP_TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /**
     * 释放锁：先校验token值再删除，避免删除其他线程持有的锁。
     */
    private void unlockWithToken(String key, String token) {
        String current = stringRedisTemplate.opsForValue().get(key);
        if (token.equals(current)) {
            stringRedisTemplate.delete(key);
        }
    }

    // ---- 商户更新与缓存清除 ----

    @Override
    public void evictShopCache(Long id) {
        String cacheKey = RedisConstants.CACHE_SHOP_KEY + id;
        stringRedisTemplate.delete(cacheKey);
        log.info("Shop cache evicted: id={}", id);
    }

    @Override
    public void updateShop(Shop shop) {
        shopMapper.updateById(shop);
        // 更新后删除缓存，下次查询自动重建
        evictShopCache(shop.getId());
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
