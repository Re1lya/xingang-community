package com.xingang.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop")
public class Shop {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Long typeId;
    private String area;
    private String address;
    /** 经度 */
    private Double x;
    /** 纬度 */
    private Double y;
    private Integer avgPrice;
    private Double score;
    private String openHours;
    private Integer comments;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
