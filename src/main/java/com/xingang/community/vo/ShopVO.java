package com.xingang.community.vo;

import lombok.Data;

/**
 * 商户详情或列表VO，包含距离字段用于GEO附近查询展示。
 */
@Data
public class ShopVO {

    private Long id;
    private String name;
    private Long typeId;
    private String area;
    private String address;
    private Double x;
    private Double y;
    private Integer avgPrice;
    private Double score;
    private String openHours;
    private Integer comments;
    /** 距离（米），仅GEO附近查询时有值 */
    private Double distance;
}
