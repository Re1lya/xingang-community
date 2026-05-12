package com.xingang.community.voucher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xingang.community.entity.SeckillVoucher;
import com.xingang.community.entity.Voucher;
import com.xingang.community.voucher.mapper.SeckillVoucherMapper;
import com.xingang.community.voucher.mapper.VoucherMapper;
import com.xingang.community.voucher.service.VoucherService;
import com.xingang.community.vo.VoucherVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoucherServiceImpl implements VoucherService {

    @Resource
    private VoucherMapper voucherMapper;

    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;

    @Override
    public List<VoucherVO> queryVoucherByShopId(Long shopId) {
        List<Voucher> vouchers = voucherMapper.selectList(
                new LambdaQueryWrapper<Voucher>()
                        .eq(Voucher::getShopId, shopId)
                        .eq(Voucher::getStatus, 1) // 仅上架券
        );

        return vouchers.stream().map(v -> {
            VoucherVO vo = new VoucherVO();
            vo.setId(v.getId());
            vo.setShopId(v.getShopId());
            vo.setTitle(v.getTitle());
            vo.setSubTitle(v.getSubTitle());
            vo.setRules(v.getRules());
            vo.setPayValue(v.getPayValue());
            vo.setActualValue(v.getActualValue());
            vo.setType(v.getType());

            // 秒杀券额外获取库存和时间
            if (v.getType() != null && v.getType() == 1) {
                SeckillVoucher sv = seckillVoucherMapper.selectById(v.getId());
                if (sv != null) {
                    vo.setStock(sv.getStock());
                    vo.setBeginTime(sv.getBeginTime());
                    vo.setEndTime(sv.getEndTime());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
