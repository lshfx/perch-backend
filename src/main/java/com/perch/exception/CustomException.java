package com.perch.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义业务异常
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomException extends RuntimeException {
    private Integer code;
    private String message;

    public CustomException(String message) {
        super(message);
        this.message = message;
        this.code = 500; // 默认业务错误码
    }

    public CustomException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}