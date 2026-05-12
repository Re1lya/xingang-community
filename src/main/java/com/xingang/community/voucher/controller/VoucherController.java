package com.xingang.community.voucher.controller;

import com.xingang.community.common.result.Result;
import com.xingang.community.voucher.service.VoucherService;
import com.xingang.community.vo.VoucherVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券查询接口。
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private VoucherService voucherService;

    /**
     * 查询商户优惠券列表。
     */
    @GetMapping("/list/{shopId}")
    public Result<List<VoucherVO>> queryVoucherByShopId(@PathVariable Long shopId) {
        List<VoucherVO> list = voucherService.queryVoucherByShopId(shopId);
        return Result.ok(list);
    }
}
