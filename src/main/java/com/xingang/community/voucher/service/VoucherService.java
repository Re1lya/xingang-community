package com.xingang.community.voucher.service;

import com.xingang.community.vo.VoucherVO;

import java.util.List;

public interface VoucherService {

    /**
     * 查询指定商户的优惠券列表，秒杀券包含库存和时间信息。
     */
    List<VoucherVO> queryVoucherByShopId(Long shopId);
}
