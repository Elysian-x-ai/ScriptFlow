package com.scriptflow.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Unified API response wrapper.
 */
@Data
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    private R() {}

    public static <T> R<T> success() {
        return result(ResultCode.SUCCESS, null);
    }

    public static <T> R<T> success(T data) {
        return result(ResultCode.SUCCESS, data);
    }

    public static <T> R<T> success(String message, T data) {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> R<T> fail() {
        return result(ResultCode.FAIL, null);
    }

    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.code = ResultCode.FAIL.getCode();
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return result(resultCode, null);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        R<T> r = new R<>();
        r.code = resultCode.getCode();
        r.message = message;
        return r;
    }

    public static <T> R<T> result(ResultCode resultCode, T data) {
        R<T> r = new R<>();
        r.code = resultCode.getCode();
        r.message = resultCode.getMessage();
        r.data = data;
        return r;
    }

    public boolean isSuccess() {
        return code == ResultCode.SUCCESS.getCode();
    }
}
