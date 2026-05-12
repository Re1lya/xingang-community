package com.xingang.community.common.constant;

/**
 * Centralized business error codes.
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    public static final String SUCCESS = "0";
    public static final String PARAM_INVALID = "PARAM_INVALID";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";

    public static final String LOGIN_FAILED = "LOGIN_FAILED";

    public static final String SHOP_NOT_FOUND = "SHOP_NOT_FOUND";

    public static final String VOUCHER_NOT_FOUND = "VOUCHER_NOT_FOUND";
    public static final String SECKILL_VOUCHER_NOT_FOUND = "SECKILL_VOUCHER_NOT_FOUND";
    public static final String SECKILL_STOCK_NOT_ENOUGH = "SECKILL_STOCK_NOT_ENOUGH";
    public static final String SECKILL_DUPLICATE_ORDER = "SECKILL_DUPLICATE_ORDER";
    public static final String SECKILL_NOT_STARTED = "SECKILL_NOT_STARTED";
    public static final String SECKILL_ENDED = "SECKILL_ENDED";
    public static final String SECKILL_QUEUE_FAILED = "SECKILL_QUEUE_FAILED";

    public static final String AI_RATE_LIMITED = "AI_RATE_LIMITED";
    public static final String AI_SERVICE_UNAVAILABLE = "AI_SERVICE_UNAVAILABLE";
    public static final String AI_KNOWLEDGE_REBUILD_FORBIDDEN = "AI_KNOWLEDGE_REBUILD_FORBIDDEN";
}

