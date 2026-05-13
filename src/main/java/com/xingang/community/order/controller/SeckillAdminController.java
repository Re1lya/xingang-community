package com.xingang.community.order.controller;

import com.xingang.community.common.result.Result;
import com.xingang.community.order.dto.PendingListInfo;
import com.xingang.community.order.dto.SeckillStockVO;
import com.xingang.community.order.service.SeckillObservabilityService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀可观测接口（纯只读，与用户秒杀接口分离）。
 *
 * <p>挂载在 /admin/seckill 路径下，与 /voucher-order/seckill 用户接口完全隔离。
 * 所有接口均为 GET 只读操作，不改变 Redis Stream 消息状态、不修改库存。</p>
 */
@RestController
@RequestMapping("/admin/seckill")
public class SeckillAdminController {

    @Resource
    private SeckillObservabilityService seckillObservabilityService;

    /**
     * 查询 stream.orders pending-list 快照。
     * 返回消息总数、最早/最晚消息ID、每条消息的deliveryCount和消息体。
     */
    @GetMapping("/pending")
    public Result<PendingListInfo> getPendingList() {
        PendingListInfo info = seckillObservabilityService.getPendingListInfo();
        return Result.ok(info);
    }

    /**
     * 查询单张秒杀券的当前Redis库存与数据库库存对照。
     */
    @GetMapping("/stock/{voucherId}")
    public Result<SeckillStockVO> getSeckillStock(@PathVariable Long voucherId) {
        SeckillStockVO vo = seckillObservabilityService.getSeckillStock(voucherId);
        return Result.ok(vo);
    }

    /**
     * 查询所有秒杀券的库存对照。
     */
    @GetMapping("/stocks")
    public Result<List<SeckillStockVO>> getAllSeckillStocks() {
        List<SeckillStockVO> list = seckillObservabilityService.getAllSeckillStocks();
        return Result.ok(list);
    }
}
