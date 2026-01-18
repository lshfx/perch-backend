package com.perch.pojo.dto.request;

import lombok.Data;

/**
 * 用户登出请求 DTO
 */
@Data
public class LogoutRequest {
    private String tokenId;
}