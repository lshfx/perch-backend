package com.perch.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.perch.exception.CustomException;
import com.perch.pojo.common.Result;
import com.perch.pojo.dto.request.LoginRequest;
import com.perch.pojo.dto.request.RefreshTokenRequest;
import com.perch.pojo.dto.request.RegisterRequest;
import com.perch.pojo.dto.response.LoginResponse;
import com.perch.service.MailService;
import com.perch.service.TokenService;
import com.perch.service.UserService;
import com.perch.utils.JwtUtils;
import jakarta.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MailService mailService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestParam @Email String email) {
        if (userService.existsByEmail(email)) {
            throw new CustomException("该邮箱已注册，请直接登录");
        }

        mailService.sendVerificationCode(email);
        return Result.success(null, "验证码已发送");
    }

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody @Validated RegisterRequest request) {
        LoginResponse loginResponse = userService.register(request);
        return Result.success(loginResponse);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        if (StringUtils.isNotBlank(request.getWechatCode())) {
            return userService.loginByWechat(request.getWechatCode());
        }
        if (StringUtils.isNotBlank(request.getEmail()) && StringUtils.isNotBlank(request.getPassword())) {
            return userService.loginByEmail(request.getEmail(), request.getPassword());
        }

        throw new CustomException(400, "登录参数不完整");
    }

    /**
     * 用户登出
     *
     * @param authHeader 登出请求头
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (StringUtils.isBlank(authHeader)) {
            return Result.success(null, "登出成功");
        }
        String token = jwtUtils.getTokenFromHeader(authHeader);
        tokenService.logout(token);

        // 清除 SecurityContext
        SecurityContextHolder.clearContext();
        return Result.success(null, "登出成功");
    }

    /**
     * 刷新 Token
     *
     * @param request 刷新请求
     * @return 新 Token
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(@RequestBody @Validated RefreshTokenRequest request) {
        Map<String, Object> newTokenInfo = tokenService.refreshToken(
                request.getTokenId(), request.getRefreshToken());
        return Result.success(newTokenInfo, "Token刷新成功");
    }

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> getCurrentUser() {
        Map<String, Object> userInfo = userService.getCurrentUserInfo();
        return Result.success(userInfo);
    }

    /**
     * 检查 Token 是否有效
     *
     * @param tokenId Token ID
     * @return 校验结果
     */
    @GetMapping("/validate/{tokenId}")
    public Result<Map<String, Object>> validateToken(@PathVariable String tokenId) {
        Map<String, Object> result = tokenService.validateTokenInfo(tokenId);
        return Result.success(result);
    }
}
