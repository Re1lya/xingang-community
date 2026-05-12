package com.xingang.community.order.service;

public interface VoucherOrderService {

    /**
     * 秒杀下单入口。
     * 线程只负责资格判断、库存预扣和消息入队，不直接写入最终订单。
     *
     * @param voucherId 秒杀券ID
     * @return 订单ID
     */
    Long seckillVoucher(Long voucherId);
}
