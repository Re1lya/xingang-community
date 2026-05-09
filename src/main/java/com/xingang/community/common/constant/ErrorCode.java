package com.xingang.community.common.constant;

/**
 * 错误码常量，集中管理所有业务错误码。
 */
public final class ErrorCode {

    private ErrorCode() {}

    // 通用
    public static final String PARAM_INVALID = "PARAM_INVALID";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    // 登录
    public static final String LOGIN_FAILED = "LOGIN_FAILED";

    // 商户
    public static final String SHOP_NOT_FOUND = "SHOP_NOT_FOUND";

    // 秒杀
    public static final String SECKILL_STOCK_NOT_ENOUGH = "SECKILL_STOCK_NOT_ENOUGH";
    public static final String SECKILL_DUPLICATE_ORDER = "SECKILL_DUPLICATE_ORDER";
    public static final String SECKILL_NOT_STARTED = "SECKILL_NOT_STARTED";
    public static final String SECKILL_ENDED = "SECKILL_ENDED";
    public static final String SECKILL_QUEUE_FAILED = "SECKILL_QUEUE_FAILED";
    public static final String SECKILL_VOUCHER_NOT_FOUND = "SECKILL_VOUCHER_NOT_FOUND";

    // 优惠券
    public static final String VOUCHER_NOT_FOUND = "VOUCHER_NOT_FOUND";
}
