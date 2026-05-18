package com.xingang.community.order.dto;

import lombok.Data;

/**
 * 秒杀库存只读查询结果，同时返回Redis库存和数据库库存供对照。
 */
@Data
public class SeckillStockVO {

    /** 秒杀券ID */
    private Long voucherId;

    /** Redis 当前库存（null表示Key不存在） */
    private Integer redisStock;

    /** 数据库库存 */
    private Integer dbStock;
}
