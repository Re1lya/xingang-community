package com.xingang.community.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券展示VO，秒杀券包含库存和时间信息。
 */
@Data
public class VoucherVO {

    private Long id;
    private Long shopId;
    private String title;
    private String subTitle;
    private String rules;
    private Long payValue;
    private Long actualValue;
    /** 优惠券类型：0=普通券, 1=秒杀券 */
    private Integer type;
    /** 秒杀券库存，普通券为空 */
    private Integer stock;
    /** 秒杀开始时间 */
    private LocalDateTime beginTime;
    /** 秒杀结束时间 */
    private LocalDateTime endTime;
}
