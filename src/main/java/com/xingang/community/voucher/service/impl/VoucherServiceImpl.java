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
     *
     * <h3>初始化策略：仅在 Redis Key 不存在时写入</h3>
     * 使用 {@code hasKey} 判断 {@code seckill:stock:{voucherId}} 是否已存在：
     * <ul>
     *   <li>不存在 → 从 MySQL 读取 stock 写入 Redis</li>
     *   <li>已存在 → 跳过，避免服务重启覆盖运行时库存</li>
     * </ul>
     *
     * <p><b>为什么不强制覆盖？</b>
     * 服务重启时 Redis 中的秒杀库存可能已经被扣减（如已售出部分），
     * 如果每次启动都从 MySQL 重新加载原始 stock 并覆盖 Redis Key，
     * 会导致已售出的库存被"回滚"，造成超卖。</p>
     *
     * <p><b>手动重置库存能力</b>属于后续增强，本轮不提供管理接口。
     * 如需重置，可通过 Redis CLI 删除对应 Key 后重启服务，或直接 SET 新值。</p>
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
