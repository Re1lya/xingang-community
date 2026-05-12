package com.xingang.community.shop.service;

import com.xingang.community.vo.ShopTypeVO;
import com.xingang.community.vo.ShopVO;

import java.util.List;

public interface ShopService {

    /**
     * 查询商户详情，优先走Redis缓存。
     */
    ShopVO queryById(Long id);

    /**
     * 查询商户分类列表，走Redis缓存。
     */
    List<ShopTypeVO> queryShopTypeList();

    /**
     * 按分类查询商户。
     * 传入经纬度时使用Redis GEO按距离查询，否则MySQL分页查询。
     */
    List<ShopVO> queryShopByType(Long typeId, Integer current, Integer pageSize, Double x, Double y);
}
