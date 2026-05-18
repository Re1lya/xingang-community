package com.xingang.community.common.constant;

/**
 * Redis Key常量，所有Redis Key前缀集中定义在此类中，禁止业务代码硬编码Key字符串。
 *
 * <pre>
 * 变量命名规则：
 *   Key模板：大写 + KEY 结尾
 *   TTL：大写 + TTL 结尾
 * </pre>
 */
public final class RedisConstants {

    private RedisConstants() {}

    // ==================== 登录 ====================
    /** 登录token，变量: {token} */
    public static final String LOGIN_TOKEN_KEY = "login:token:";

    // ==================== 商户缓存 ====================
    /** 商户详情缓存，变量: {id} */
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    /** 商户分类缓存 */
    public static final String CACHE_SHOP_TYPE_KEY = "cache:shop:type";
    /** 商户缓存重建互斥锁，变量: {id} */
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    /** 商户GEO坐标，变量: {typeId} */
    public static final String SHOP_GEO_KEY = "shop:geo:";

    // ==================== 秒杀 ====================
    /** 秒杀券Redis库存，变量: {voucherId} */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    /** 已抢券用户集合（一人一单校验），变量: {voucherId} */
    public static final String SECKILL_ORDER_KEY = "seckill:order:";
    /** 秒杀订单Redis Stream */
    public static final String STREAM_ORDERS_KEY = "stream.orders";
    /** 秒杀订单死信队列 Stream，存储超过最大投递次数的失败订单 */
    public static final String STREAM_ORDERS_DLQ_KEY = "stream.orders.dlq";
    /** 用户维度分布式锁，变量: {userId} */
    public static final String LOCK_ORDER_KEY = "lock:order:";

    // ==================== TTL ====================
    /** 商户缓存TTL（分钟） */
    public static final long CACHE_SHOP_TTL_MINUTES = 30;
    /** 空值缓存TTL（分钟），防止缓存穿透 */
    public static final long CACHE_NULL_TTL_MINUTES = 2;
    /** 商户缓存重建互斥锁TTL（秒） */
    public static final long LOCK_SHOP_TTL_SECONDS = 10;
    /** 登录token TTL（分钟） */
    public static final long LOGIN_TOKEN_TTL_MINUTES = 30;
    /** Redisson用户锁TTL（秒） */
    public static final long LOCK_ORDER_TTL_SECONDS = 30;
}
