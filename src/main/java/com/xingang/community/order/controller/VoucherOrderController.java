package com.xingang.community.order.controller;

import com.xingang.community.common.result.Result;
import com.xingang.community.order.service.VoucherOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀下单接口。
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private VoucherOrderService voucherOrderService;

    /**
     * 秒杀下单。
     * 返回订单ID表示已成功入队，最终订单由后台消费者异步创建。
     */
    @PostMapping("/seckill/{voucherId}")
    public Result<Long> seckill(@PathVariable Long voucherId) {
        Long orderId = voucherOrderService.seckillVoucher(voucherId);
        return Result.ok(orderId);
    }
}
