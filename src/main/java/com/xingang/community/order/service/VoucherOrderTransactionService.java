package com.xingang.community.order.service;

/**
 * 秒杀订单事务服务，独立Bean确保 @Transactional 通过Spring AOP代理生效。
 */
public interface VoucherOrderTransactionService {

    /**
     * 在事务中二次校验并创建订单。
     *
     * @param orderId   秒杀入口生成的订单ID
     * @param userId    用户ID
     * @param voucherId 秒杀券ID
     */
    void createOrderInTransaction(Long orderId, Long userId, Long voucherId);
}
