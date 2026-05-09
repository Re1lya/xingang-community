package com.xingang.community.common.result;

public class Result<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode("OK");
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
