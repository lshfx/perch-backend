package com.perch.controller;

import com.perch.entity.common.Result;
import com.perch.service.TokenService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private TokenService tokenService;

    /**
     * 用户注册
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            // TODO: 这里应该实现完整的注册逻辑
            // 1. 参数验证
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return Result.error("密码长度不能少于6位");
            }
            if (request.getEmail() == null || !request.getEmail().contains("@")) {
                return Result.error("邮箱格式不正确");
            }

            // TODO: 2. 检查用户名/邮箱是否已存在
            // if (userService.existsByUsername(request.getUsername())) {
            //     return Result.error("用户名已存在");
            // }
            // if (userService.existsByEmail(request.getEmail())) {
            //     return Result.error("邮箱已被注册");
            // }

            // TODO: 3. 创建用户
            // User user = new User();
            // user.setUsername(request.getUsername());
            // user.setPassword(passwordEncoder.encode(request.getPassword()));
            // user.setEmail(request.getEmail());
            // user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
            // user.setCreateTime(LocalDateTime.now());
            // userService.save(user);

            // TODO: 4. 注册成功后自动登录，返回Token
            Map<String, Object> tokenInfo = tokenService.createToken(
                1L, // TODO: 使用真实的用户ID
                request.getUsername(),
                request.getDeviceInfo() != null ? request.getDeviceInfo() : "未知设备"
            );

            log.info("用户 {} 注册成功", request.getUsername());
            return Result.success(tokenInfo, "注册成功");

        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage());
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            // TODO: 这里应该验证用户名密码，现在简化处理
            if (!"admin".equals(request.getUsername()) || !"admin123".equals(request.getPassword())) {
                return Result.error("用户名或密码错误");
            }

            // 创建 Token
            Map<String, Object> tokenInfo = tokenService.createToken(
                1L,
                request.getUsername(),
                request.getDeviceInfo() != null ? request.getDeviceInfo() : "未知设备"
            );

            log.info("用户 {} 登录成功", request.getUsername());
            return Result.success(tokenInfo, "登录成功");

        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出
     * @param request 登出请求
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestBody LogoutRequest request) {
        try {
            if (request.getTokenId() != null) {
                tokenService.revokeToken(request.getTokenId());
                log.info("Token {} 已登出", request.getTokenId());
            }

            // 清除 SecurityContext
            SecurityContextHolder.clearContext();
            return Result.success(null, "登出成功");

        } catch (Exception e) {
            log.error("登出失败: {}", e.getMessage());
            return Result.error("登出失败: " + e.getMessage());
        }
    }

    /**
     * 刷新 Token
     * @param request 刷新请求
     * @return 新 Token
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            Map<String, Object> newTokenInfo = tokenService.refreshToken(request.getTokenId());

            if (newTokenInfo != null) {
                log.info("Token 刷新成功: {}", request.getTokenId());
                return Result.success(newTokenInfo, "Token 刷新成功");
            } else {
                return Result.error("Token 无效或已过期");
            }

        } catch (Exception e) {
            log.error("Token 刷新失败: {}", e.getMessage());
            return Result.error("Token 刷新失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     * @return 用户信息
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", authentication.getName());
                userInfo.put("authorities", authentication.getAuthorities());

                return Result.success(userInfo);
            } else {
                return Result.error("未登录");
            }

        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查 Token 是否有效
     * @param tokenId Token ID
     * @return 验证结果
     */
    @GetMapping("/validate/{tokenId}")
    public Result<Map<String, Object>> validateToken(@PathVariable String tokenId) {
        try {
            boolean isValid = tokenService.validateToken(tokenId);

            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("tokenId", tokenId);

            return Result.success(result);

        } catch (Exception e) {
            log.error("验证 Token 失败: {}", e.getMessage());
            return Result.error("验证 Token 失败: " + e.getMessage());
        }
    }

    // 内部请求类
    @Getter
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String nickname;
        private String deviceInfo;

        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
        public void setEmail(String email) { this.email = email; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    }

    @Getter
    public static class LoginRequest {
        private String username;
        private String password;
        private String deviceInfo;

        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
        public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    }

    @Getter
    public static class LogoutRequest {
        private String tokenId;

        public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    }

    @Getter
    public static class RefreshTokenRequest {
        private String tokenId;

        public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    }
}