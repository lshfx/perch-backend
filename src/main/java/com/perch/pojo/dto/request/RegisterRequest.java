package com.perch.pojo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求 DTO
 */
@Data
public class RegisterRequest {

    // --- 邮箱注册组 ---

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;

    /**
     * 邮箱验证码 (为了区分，建议明确命名)
     */
    private String emailVerifyCode;

    // --- 微信注册组 ---

    /**
     * 微信临时登录凭证 (前端 wx.login 获取)
     * 后端用这个换取 openid，绝对不能让前端直接传 openid
     */
    private String wechatCode;

    // --- 公共字段 ---
    private String nickname;
    private String avatarUrl;

    private String deviceInfo;
}