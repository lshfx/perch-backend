package com.perch.service.impl;

import com.alibaba.fastjson2.JSON;
import com.perch.constants.RedisConstants;
import com.perch.exception.CustomException;
import com.perch.service.TokenService;
import com.perch.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Token 管理服务实现
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建令牌
     * @param userId 用户id
     * @param username 用户名
     * @param deviceInfo 设备信息
     * @return: java.util.Map<java.lang.String,java.lang.Object>
     */
    @Override
    public Map<String, Object> createToken(Long userId, String username, String deviceInfo) {
        // 生成唯一的 Token ID
        String tokenId = UUID.randomUUID().toString().replace("-", "");

        // 创建 JWT Token（将 tokenId 放入 claims 中）
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenId", tokenId);
        claims.put("userId", userId);

        String token = jwtUtils.generateToken(username, claims);
        String refreshToken = jwtUtils.generateRefreshToken(username);

        // 存储 Token 信息到 Redis
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("tokenId", tokenId);
        tokenInfo.put("userId", userId);
        tokenInfo.put("username", username);
        tokenInfo.put("deviceInfo", deviceInfo != null ? deviceInfo : "未知设备");
        tokenInfo.put("loginTime", LocalDateTime.now().format(DATE_FORMATTER));
        tokenInfo.put("lastAccessTime", LocalDateTime.now().format(DATE_FORMATTER));
        tokenInfo.put("refreshToken", refreshToken);

        // 存储 Token（设置过期时间与 refreshToken 一致，便于刷新）
        String tokenKey = RedisConstants.buildTokenKey(tokenId);
        redisTemplate.opsForHash().putAll(tokenKey, tokenInfo);
        redisTemplate.expire(tokenKey, jwtUtils.getRefreshExpiration(), TimeUnit.MILLISECONDS);

        // 将 Token 添加到用户的 Token 列表
        String userTokensKey = RedisConstants.buildUserTokenListKey(userId);
        redisTemplate.opsForSet().add(userTokensKey, tokenId);
        redisTemplate.expire(userTokensKey, jwtUtils.getRefreshExpiration(), TimeUnit.MILLISECONDS);

        // 更新在线统计
        updateOnlineStats();

        // 返回 Token 信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("tokenId", tokenId);
        result.put("expiration", jwtUtils.getExpiration());
        result.put("tokenPrefix", jwtUtils.getTokenPrefix());

        log.info("用户 {} 创建新 Token: {}", username, tokenId);
        return result;
    }

    /***
     * 验证用户令牌有效性
     * @param tokenId 令牌ID
     * @return: boolean
     */
    @Override
    public boolean validateToken(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        String tokenKey = RedisConstants.buildTokenKey(tokenId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }

    /**
     * 撤销指定的用户令牌
     * @param tokenId 令牌ID
     * @return: void
     */
    @Override
    public void revokeToken(String tokenId) {
        if (tokenId == null) {
            return;
        }
        String tokenKey = RedisConstants.buildTokenKey(tokenId);
        Map<Object, Object> tokenInfo = redisTemplate.opsForHash().entries(tokenKey);

        if (!tokenInfo.isEmpty()) {
            Long userId = Long.valueOf(tokenInfo.get("userId").toString());

            // 从用户的 Token 列表中移除
            String userTokensKey = RedisConstants.buildUserTokenListKey(userId);
            redisTemplate.opsForSet().remove(userTokensKey, tokenId);

            // 删除 Token
            redisTemplate.delete(tokenKey);

            log.info("Token {} 已撤销", tokenId);
            updateOnlineStats();
        }
    }

    /**
     * 撤销用户所有的登录令牌
     * @param userId 用户id
     * @return: void
     */
    @Override
    public void revokeAllUserTokens(Long userId) {
        String userTokensKey = RedisConstants.buildUserTokenListKey(userId);
        Set<Object> tokenIds = redisTemplate.opsForSet().members(userTokensKey);

        if (tokenIds != null && !tokenIds.isEmpty()) {
            // 删除所有 Token
            List<String> tokenKeys = new ArrayList<>();
            for (Object tokenId : tokenIds) {
                tokenKeys.add(RedisConstants.buildTokenKey(tokenId.toString()));
            }

            if (!tokenKeys.isEmpty()) {
                redisTemplate.delete(tokenKeys);
                log.info("用户 {} 的 {} 个 Token 已全部撤销", userId, tokenKeys.size());
            }

            // 删除用户的 Token 列表
            redisTemplate.delete(userTokensKey);
            updateOnlineStats();
        }
    }

    /**
     * 获取用户的所有登录令牌
     * @param userId 用户id
     * @return: java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     */
    @Override
    public List<Map<String, Object>> getUserTokens(Long userId) {
        List<Map<String, Object>> tokens = new ArrayList<>();
        String userTokensKey = RedisConstants.buildUserTokenListKey(userId);
        Set<Object> tokenIds = redisTemplate.opsForSet().members(userTokensKey);

        if (tokenIds == null) {
            return tokens;
        }

        for (Object tokenId : tokenIds) {
            String tokenKey = RedisConstants.buildTokenKey(tokenId.toString());
            Map<Object, Object> tokenInfo = redisTemplate.opsForHash().entries(tokenKey);

            if (!tokenInfo.isEmpty()) {
                Map<String, Object> tokenMap = new HashMap<>();
                tokenInfo.forEach((key, value) -> tokenMap.put(key.toString(), value));
                tokens.add(tokenMap);
            }
        }

        return tokens;
    }

    /**
     *  获取用户在线情况
     * @return: java.util.Map<java.lang.String,java.lang.Object>
     */
    @Override
    public Map<String, Object> getOnlineStats() {
        Map<String, Object> stats = new HashMap<>();
        Object onlineCount = redisTemplate.opsForValue().get(RedisConstants.ONLINE_STATS_KEY);
        stats.put("onlineCount", onlineCount != null ? Integer.parseInt(onlineCount.toString()) : 0);
        stats.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return stats;
    }

    /**
     * 刷新令牌
     * @param oldTokenId
     * @return: java.util.Map<java.lang.String,java.lang.Object>
     */
    @Override
    public Map<String, Object> refreshToken(String oldTokenId, String refreshToken) {
        // 1. 基础参数校验
        if (!StringUtils.hasText(oldTokenId) || !StringUtils.hasText(refreshToken)) {
            throw new CustomException(400, "TokenId或RefreshToken不能为空");
        }

        // 2. 查询 Redis
        String tokenKey = RedisConstants.buildTokenKey(oldTokenId);
        Map<Object, Object> tokenInfo = redisTemplate.opsForHash().entries(tokenKey);

        if (tokenInfo.isEmpty()) {
            throw new CustomException(401, "Token无效或已过期");
        }

        // 3. 校验 Refresh Token 是否匹配
        Object storedRefreshToken = tokenInfo.get("refreshToken");
        // 注意：request中的refreshToken上面已经校验过 hasText，所以这里不用判 null，只需判 stored
        if (storedRefreshToken == null || !storedRefreshToken.toString().equals(refreshToken)) {
            log.warn("Refresh Token 校验失败: tokenId={}", oldTokenId);
            throw new CustomException(401, "RefreshToken无效或不匹配");
        }

        // 4. 校验 Refresh Token 是否过期
        // 【修复】去掉取反符号 '!'，过期了才抛异常
        if (jwtUtils.isTokenExpired(refreshToken)) {
            log.warn("Refresh Token 已过期: tokenId={}", oldTokenId);
            // 这里可以考虑顺手把 Redis 里的脏数据清掉：revokeToken(oldTokenId);
            throw new CustomException(401, "RefreshToken已过期");
        }

        // 5. 校验用户是否匹配
        String username = (String) tokenInfo.get("username");
        String refreshUsername = jwtUtils.getUsernameFromToken(refreshToken);

        if (username == null || !username.equals(refreshUsername)) {
            log.warn("Refresh Token 用户不匹配: tokenId={}", oldTokenId);
            throw new CustomException(401, "RefreshToken用户不匹配");
        }

        // 6. 提取其他信息 (建议做非空检查)
        Object userIdObj = tokenInfo.get("userId");
        if (userIdObj == null) {
            log.error("Redis数据异常，缺失userId: tokenId={}", oldTokenId);
            throw new CustomException(500, "会话数据异常");
        }
        Long userId = Long.valueOf(userIdObj.toString());
        String deviceInfo = (String) tokenInfo.get("deviceInfo");

        // 7. 执行刷新 (撤销旧的 -> 生成新的)
        revokeToken(oldTokenId);

        return createToken(userId, username, deviceInfo);
    }

    @Override
    public Map<String, Object> validateTokenInfo(String tokenId) {
        if (!StringUtils.hasText(tokenId)) {
            throw new CustomException(400, "TokenId不能为空");
        }

        boolean isValid = validateToken(tokenId);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", isValid);
        result.put("tokenId", tokenId);
        return result;
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            throw new CustomException(401, "未提供有效Token");
        }

        Claims claims = jwtUtils.getAllClaimsFromToken(token);
        if (claims == null || claims.get("tokenId") == null) {
            throw new CustomException(401, "Token无效或已过期");
        }

        String tokenId = claims.get("tokenId").toString();
        revokeToken(tokenId);
        log.info("Token {} 已登出", tokenId);
    }

    /**
     * 更新在线统计
     */
    private void updateOnlineStats() {
        // 统计所有 Token Key 的数量
        Set<String> tokenKeys = redisTemplate.keys(RedisConstants.TOKEN_PREFIX + "*");
        int onlineCount = tokenKeys != null ? tokenKeys.size() : 0;

        redisTemplate.opsForValue().set(RedisConstants.ONLINE_STATS_KEY, onlineCount, 24, TimeUnit.HOURS);
    }

    /**
     * 更新 Token 最后访问时间
     * @param tokenId Token ID
     */
    @Override
    public void updateLastAccessTime(String tokenId, Long userId) {
        String tokenKey = RedisConstants.buildTokenKey(tokenId);
        redisTemplate.opsForHash().put(tokenKey, "lastAccessTime",
            LocalDateTime.now().format(DATE_FORMATTER));
        // 每次请求刷新 Redis 过期时间，确保活跃用户可继续刷新令牌
        redisTemplate.expire(tokenKey, jwtUtils.getRefreshExpiration(), TimeUnit.MILLISECONDS);

        if (userId != null) {
            String userTokensKey = RedisConstants.buildUserTokenListKey(userId);
            redisTemplate.expire(userTokensKey, jwtUtils.getRefreshExpiration(), TimeUnit.MILLISECONDS);
        }
    }
}
