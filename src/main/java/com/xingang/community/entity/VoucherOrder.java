package com.xingang.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券订单表。
 * 建议建立唯一索引 uk_user_voucher(user_id, voucher_id) 从数据库层防止一人多单。
 */
@Data
@TableName("voucher_order")
public class VoucherOrder {

    /** 订单ID，由雪花算法在秒杀入口生成后传入Lua脚本 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long voucherId;
    /** 订单状态：0=待确认, 1=已创建 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
