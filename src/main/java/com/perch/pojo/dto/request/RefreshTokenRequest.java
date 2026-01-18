package com.perch.pojo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 Token 请求 DTO
 */
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "TokenId不能为空")
    private String tokenId;
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
