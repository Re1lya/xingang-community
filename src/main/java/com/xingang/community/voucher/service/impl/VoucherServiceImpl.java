package com.xingang.community.voucher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xingang.community.common.constant.RedisConstants;
import com.xingang.community.entity.SeckillVoucher;
import com.xingang.community.entity.Voucher;
import com.xingang.community.voucher.mapper.SeckillVoucherMapper;
import com.xingang.community.voucher.mapper.VoucherMapper;
import com.xingang.community.voucher.service.VoucherService;
import com.xingang.community.vo.VoucherVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VoucherServiceImpl implements VoucherService {

    @Resource
    private VoucherMapper voucherMapper;

    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 服务启动时自动将 seckill_voucher.stock 初始化到 Redis。
     * 仅初始化不存在库存key的秒杀券，避免重复写入覆盖运行时库存。
     */
    @PostConstruct
    public void initSeckillStockToRedis() {
        List<SeckillVoucher> seckillVouchers = seckillVoucherMapper.selectList(
                new LambdaQueryWrapper<SeckillVoucher>()
                        .gt(SeckillVoucher::getEndTime, java.time.LocalDateTime.now())
        );
        for (SeckillVoucher sv : seckillVouchers) {
            String stockKey = RedisConstants.SECKILL_STOCK_KEY + sv.getVoucherId();
            Boolean exists = stringRedisTemplate.hasKey(stockKey);
            if (Boolean.FALSE.equals(exists)) {
                stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(sv.getStock()));
                log.info("Seckill stock initialized: voucherId={}, stock={}", sv.getVoucherId(), sv.getStock());
            }
        }
    }

    @Override
    public List<VoucherVO> queryVoucherByShopId(Long shopId) {
        List<Voucher> vouchers = voucherMapper.selectList(
                new LambdaQueryWrapper<Voucher>()
                        .eq(Voucher::getShopId, shopId)
                        .eq(Voucher::getStatus, 1) // 仅上架券
        );

        return vouchers.stream().map(v -> {
            VoucherVO vo = new VoucherVO();
            vo.setId(v.getId());
            vo.setShopId(v.getShopId());
            vo.setTitle(v.getTitle());
            vo.setSubTitle(v.getSubTitle());
            vo.setRules(v.getRules());
            vo.setPayValue(v.getPayValue());
            vo.setActualValue(v.getActualValue());
            vo.setType(v.getType());

            // 秒杀券额外获取库存和时间
            if (v.getType() != null && v.getType() == 1) {
                SeckillVoucher sv = seckillVoucherMapper.selectById(v.getId());
                if (sv != null) {
                    vo.setStock(sv.getStock());
                    vo.setBeginTime(sv.getBeginTime());
                    vo.setEndTime(sv.getEndTime());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
