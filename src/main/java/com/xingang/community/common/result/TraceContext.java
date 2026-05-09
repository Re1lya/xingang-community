package com.xingang.community.common.result;

import java.util.UUID;

/**
 * traceId上下文工具，用于在请求链路中透传或生成traceId。
 */
public class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    public static String getTraceId() {
        String tid = TRACE_ID.get();
        if (tid == null || tid.isEmpty()) {
            tid = "trace-" + UUID.randomUUID().toString().substring(0, 8);
            TRACE_ID.set(tid);
        }
        return tid;
    }

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
