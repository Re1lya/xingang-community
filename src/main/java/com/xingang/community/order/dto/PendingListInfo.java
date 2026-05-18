package com.xingang.community.order.dto;

import lombok.Data;

import java.util.List;

/**
 * pending-list 观测快照（只读，来自 XPENDING 汇总 + 单条详情）。
 */
@Data
public class PendingListInfo {

    /** Stream Key */
    private String streamKey;

    /** 消费组名称 */
    private String consumerGroup;

    /** pending 消息总数 */
    private Long totalPending;

    /** 最早pending消息ID */
    private String lowId;

    /** 最晚pending消息ID */
    private String highId;

    /** 单条消息详情（最多50条） */
    private List<PendingMessageDetail> messages;
}
