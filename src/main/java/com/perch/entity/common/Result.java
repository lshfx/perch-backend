package com.perch.entity.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果类
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应码：1成功，0和其它数字为失败
     */
    private Integer code;

    /**
     * 错误信息
     */
    private String msg;

    /**
     * 数据
     */
    private T data;

    /**
     * 成功响应（无数据）
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        return result;
    }

    /**
     * 成功响应（带数据）
     * @param data 数据
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.data = data;
        result.code = 1;
        return result;
    }

    /**
     * 成功响应（带数据和消息）
     * @param data 数据
     * @param msg 消息
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data, String msg) {
        Result<T> result = new Result<>();
        result.data = data;
        result.msg = msg;
        result.code = 1;
        return result;
    }

    /**
     * 失败响应（带错误信息）
     * @param msg 错误信息
     * @param <T> 数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.msg = msg;
        result.code = 0;
        return result;
    }

    /**
     * 失败响应（带错误码和错误信息）
     * @param code 错误码
     * @param msg 错误信息
     * @param <T> 数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        return result;
    }

    /**
     * 自定义响应
     * @param code 响应码
     * @param msg 消息
     * @param data 数据
     * @param <T> 数据类型
     * @return 自定义结果
     */
    public static <T> Result<T> result(Integer code, String msg, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        result.data = data;
        return result;
    }

    /**
     * 判断是否成功
     * @return 是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 1;
    }

    /**
     * 判断是否失败
     * @return 是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }
}