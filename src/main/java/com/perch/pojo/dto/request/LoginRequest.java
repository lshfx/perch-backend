package com.perch.pojo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录请求 DTO
 */
@Data
public class LoginRequest {

    // ========== 方式 A：邮箱密码登录 ==========

    // 不加 @NotBlank，因为如果是微信登录，这里是空的
    @Email(message = "邮箱格式不正确")
    private String email;

    // 不加 @NotBlank，同上
    private String password;


    // ========== 方式 B：微信一键登录 ==========

    /**
     * 微信登录凭证 (code)
     * 注意：前端调用 wx.login() 拿到的是一个临时的 code
     * 后端拿着这个 code 去找微信服务器换取 openid
     * ⚠️ 安全提示：尽量不要直接传 openid，那样容易被黑客抓包伪造身份！
     */
    private String wechatCode;
}