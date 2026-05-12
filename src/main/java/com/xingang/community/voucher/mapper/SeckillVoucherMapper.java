package com.xingang.community.voucher.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xingang.community.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

    /**
     * 乐观扣减秒杀库存，条件: stock > 0
     * @param voucherId 优惠券ID
     * @return 影响行数，0表示库存不足
     */
    @Update("UPDATE seckill_voucher SET stock = stock - 1 WHERE voucher_id = #{voucherId} AND stock > 0")
    int deductStock(@Param("voucherId") Long voucherId);
}
