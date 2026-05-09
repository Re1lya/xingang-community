package com.xingang.community.common.util;

/**
 * 用户上下文，用于在业务层获取当前登录用户。
 * Controller通过拦截器或直接设置，Service通过此类读取。
 *
 * <p>异步线程使用用户上下文时，必须显式传递必要字段，不得依赖ThreadLocal自动继承。</p>
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
