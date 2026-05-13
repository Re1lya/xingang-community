package com.xingang.community.order.dto;

import lombok.Data;

/**
 * 单条pending消息详情，取自 XPENDING + XRANGE（只读，不改变pending状态）。
 */
@Data
public class PendingMessageDetail {

    /** Stream消息ID */
    private String messageId;

    /** 消费者名称 */
    private String consumer;

    /** 空闲时间（毫秒） */
    private Long idleTimeMs;

    /** 投递次数 */
    private Long deliveryCount;

    /** 订单ID（来自消息体，通过XRANGE只读读取） */
    private String orderId;

    /** 用户ID（来自消息体） */
    private String userId;

    /** 秒杀券ID（来自消息体） */
    private String voucherId;
}
