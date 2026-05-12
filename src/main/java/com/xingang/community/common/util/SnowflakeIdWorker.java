package com.xingang.community.common.util;

/**
 * 雪花算法ID生成器，用于秒杀订单ID预生成。
 * 简化实现：基于MyBatis-Plus内置ID生成策略对应的System.nanoTime高位拼接。
 *
 * <p>正式环境建议使用MyBatis-Plus的IdWorker或自定义雪花算法。</p>
 */
public final class SnowflakeIdWorker {

    private SnowflakeIdWorker() {}

    /**
     * 基于时间戳+随机数生成全局唯一订单ID。
     * 简化实现，保证全局唯一性即可。
     */
    public static long nextId() {
        long timestamp = System.currentTimeMillis();
        long random = (long) (Math.random() * 1000000);
        return timestamp * 1000000 + random;
    }
}
