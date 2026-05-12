package com.xingang.community.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xingang.community.common.constant.ErrorCode;
import com.xingang.community.common.exception.BusinessException;
import com.xingang.community.entity.VoucherOrder;
import com.xingang.community.order.mapper.VoucherOrderMapper;
import com.xingang.community.order.service.VoucherOrderTransactionService;
import com.xingang.community.voucher.mapper.SeckillVoucherMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 独立事务Bean，供 Stream消费者 通过Spring代理调用 @Transactional 方法。
 */
@Slf4j
@Service
public class VoucherOrderTransactionServiceImpl implements VoucherOrderTransactionService {

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrderInTransaction(Long orderId, Long userId, Long voucherId) {
        // 1. 二次校验一人一单：检查订单是否已存在
        Long existCount = voucherOrderMapper.selectCount(
                new LambdaQueryWrapper<VoucherOrder>()
                        .eq(VoucherOrder::getUserId, userId)
                        .eq(VoucherOrder::getVoucherId, voucherId)
        );
        if (existCount > 0) {
            log.warn("Duplicate order prevented in transaction: userId={}, voucherId={}", userId, voucherId);
            return;
        }

        // 2. 乐观扣减MySQL库存 (WHERE stock > 0)
        int rows = seckillVoucherMapper.deductStock(voucherId);
        if (rows == 0) {
            log.warn("DB stock deduct failed (stock=0): voucherId={}", voucherId);
            throw new BusinessException(ErrorCode.SECKILL_STOCK_NOT_ENOUGH, "库存不足");
        }

        // 3. 插入订单记录
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        order.setStatus(1); // 已创建
        voucherOrderMapper.insert(order);

        log.info("Order created in DB: orderId={}, userId={}, voucherId={}", orderId, userId, voucherId);
    }
}
