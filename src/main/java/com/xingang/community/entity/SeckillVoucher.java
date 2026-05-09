package com.xingang.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀券表，与优惠券表通过voucherId关联。
 * 数据库扣减库存必须使用 stock > 0 条件。
 */
@Data
@TableName("seckill_voucher")
public class SeckillVoucher {

    @TableId(type = IdType.ASSIGN_ID)
    private Long voucherId;
    /** 数据库剩余库存 */
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
