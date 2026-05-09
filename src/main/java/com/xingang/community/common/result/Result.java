package com.xingang.community.common.result;

import lombok.Data;

import java.time.Instant;

/**
 * 统一响应结构，所有HTTP接口必须返回此格式。
 */
@Data
public class Result<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private String traceId;
    private long timestamp;

    private Result() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.success = true;
        r.code = "0";
        r.message = "success";
        r.data = data;
        r.traceId = TraceContext.getTraceId();
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> r = new Result<>();
        r.success = false;
        r.code = code;
        r.message = message;
        r.data = null;
        r.traceId = TraceContext.getTraceId();
        return r;
    }
}
