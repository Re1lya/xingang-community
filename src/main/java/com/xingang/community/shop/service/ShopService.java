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

    /**
     * 清除指定商户的Redis缓存。
     * 商户更新后调用，后续查询将重建缓存。
     */
    void evictShopCache(Long id);

    /**
     * 更新商户信息并自动清除缓存。
     * 当前无Controller层更新入口，预留供后续使用。
     */
    void updateShop(com.xingang.community.entity.Shop shop);
}
