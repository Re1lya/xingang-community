package com.xingang.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券表。type: 0=普通券, 1=秒杀券
 */
@Data
@TableName("voucher")
public class Voucher {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long shopId;
    private String title;
    private String subTitle;
    private String rules;
    private Long payValue;
    private Long actualValue;
    /** 优惠券类型：0=普通券, 1=秒杀券 */
    private Integer type;
    /** 状态：0=下架, 1=上架 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
