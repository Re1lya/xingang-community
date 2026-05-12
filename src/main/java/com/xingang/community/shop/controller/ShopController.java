package com.xingang.community.shop.controller;

import com.xingang.community.common.result.Result;
import com.xingang.community.shop.service.ShopService;
import com.xingang.community.vo.ShopTypeVO;
import com.xingang.community.vo.ShopVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户查询接口。
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    private ShopService shopService;

    /**
     * 商户详情。
     */
    @GetMapping("/{id}")
    public Result<ShopVO> queryShopById(@PathVariable Long id) {
        ShopVO vo = shopService.queryById(id);
        if (vo == null) {
            return Result.fail("SHOP_NOT_FOUND", "商户不存在");
        }
        return Result.ok(vo);
    }

    /**
     * 按分类查询商户。传入经纬度时启用Redis GEO附近查询。
     */
    @GetMapping("/of/type")
    public Result<List<ShopVO>> queryShopByType(
            @RequestParam Long typeId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Double x,
            @RequestParam(required = false) Double y) {
        List<ShopVO> list = shopService.queryShopByType(typeId, current, pageSize, x, y);
        return Result.ok(list);
    }
}
