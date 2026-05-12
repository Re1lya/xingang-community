package com.xingang.community.common.result;

import com.xingang.community.common.constant.ErrorCode;

import java.time.Instant;

/**
 * Unified HTTP response body for non-SSE APIs.
 */
public class Result<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private String traceId;
    private long timestamp;

    private Result() {
        this.timestamp = Instant.now().toEpochMilli();
        this.traceId = TraceContext.getTraceId();
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.success = true;
        result.code = ErrorCode.SUCCESS;
        result.message = "success";
        result.data = data;
        return result;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.success = false;
        result.code = code;
        result.message = message;
        result.data = null;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

