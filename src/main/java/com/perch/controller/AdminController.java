package com.perch.controller;

import com.perch.entity.common.Result;
import com.perch.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private TokenService tokenService;

    /**
     * 强制用户下线（撤销所有 Token）
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/kick-user/{userId}")
    public Result<Map<String, Object>> kickUser(@PathVariable Long userId) {
        try {
            tokenService.revokeAllUserTokens(userId);
            log.info("管理员强制用户 {} 下线", userId);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("timestamp", System.currentTimeMillis());

            return Result.success(data, "用户已强制下线");
        } catch (Exception e) {
            log.error("强制用户下线失败: {}", e.getMessage());
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 撤销特定 Token
     * @param tokenId Token ID
     * @return 操作结果
     */
    @PostMapping("/revoke-token/{tokenId}")
    public Result<Map<String, Object>> revokeToken(@PathVariable String tokenId) {
        try {
            tokenService.revokeToken(tokenId);
            log.info("管理员撤销 Token: {}", tokenId);

            Map<String, Object> data = new HashMap<>();
            data.put("tokenId", tokenId);
            data.put("timestamp", System.currentTimeMillis());

            return Result.success(data, "Token 已撤销");
        } catch (Exception e) {
            log.error("撤销 Token 失败: {}", e.getMessage());
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的所有在线 Token
     * @param userId 用户ID
     * @return Token 列表
     */
    @GetMapping("/user-tokens/{userId}")
    public Result<Map<String, Object>> getUserTokens(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> tokens = tokenService.getUserTokens(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("tokens", tokens);
            data.put("count", tokens.size());
            data.put("userId", userId);

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取用户 Token 列表失败: {}", e.getMessage());
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取在线用户统计
     * @return 统计信息
     */
    @GetMapping("/online-stats")
    public Result<Map<String, Object>> getOnlineStats() {
        try {
            Map<String, Object> stats = tokenService.getOnlineStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取在线统计失败: {}", e.getMessage());
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 检查 Token 是否有效
     * @param tokenId Token ID
     * @return 验证结果
     */
    @GetMapping("/validate-token/{tokenId}")
    public Result<Map<String, Object>> validateToken(@PathVariable String tokenId) {
        try {
            boolean isValid = tokenService.validateToken(tokenId);

            Map<String, Object> data = new HashMap<>();
            data.put("valid", isValid);
            data.put("tokenId", tokenId);

            return Result.success(data);
        } catch (Exception e) {
            log.error("验证 Token 失败: {}", e.getMessage());
            return Result.error("验证失败: " + e.getMessage());
        }
    }
}