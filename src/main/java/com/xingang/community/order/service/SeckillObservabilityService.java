package com.xingang.community.order.service;

import com.xingang.community.order.dto.PendingListInfo;
import com.xingang.community.order.dto.SeckillStockVO;

import java.util.List;

/**
 * 秒杀可观测服务（纯只读，不含任何写入/ACK/重置操作）。
 */
public interface SeckillObservabilityService {

    /**
     * 获取 stream.orders 消费组的 pending-list 快照。
     * 使用 XPENDING + XRANGE，不改变消息状态。
     */
    PendingListInfo getPendingListInfo();

    /**
     * 查询单张秒杀券的 Redis 库存与数据库库存对照。
     */
    SeckillStockVO getSeckillStock(Long voucherId);

    /**
     * 查询所有未过期秒杀券的库存对照。
     */
    List<SeckillStockVO> getAllSeckillStocks();
}
