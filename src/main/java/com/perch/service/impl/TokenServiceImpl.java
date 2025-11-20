package com.perch.service.impl;

import com.alibaba.fastjson2.JSON;
import com.perch.constants.RedisConstants;
import com.perch.service.TokenService;
import com.perch.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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

        // 存储 Token（设置过期时间与 JWT 一致）
        String tokenKey = RedisConstants.buildTokenKey(tokenId);
        redisTemplate.opsForHash().putAll(tokenKey, tokenInfo);
        redisTemplate.expire(tokenKey, jwtUtils.getExpiration(), TimeUnit.MILLISECONDS);

        // 将 Token 添加到用户的 Token 列表
        String userTokensKey = RedisConstants.buildUserTokenListKey(userId);
        redisTemplate.opsForSet().add(userTokensKey, tokenId);
        redisTemplate.expire(userTokensKey, jwtUtils.getExpiration(), TimeUnit.MILLISECONDS);

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

        try {
            String tokenKey = RedisConstants.buildTokenKey(tokenId);
            return redisTemplate.hasKey(tokenKey);
        } catch (Exception e) {
            log.error("验证 Token 失败: {}", e.getMessage());
            return false;
        }
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

        try {
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
        } catch (Exception e) {
            log.error("撤销 Token 失败: {}", e.getMessage());
        }
    }

    /**
     * 撤销用户所有的登录令牌
     * @param userId 用户id
     * @return: void
     */
    @Override
    public void revokeAllUserTokens(Long userId) {
        try {
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
        } catch (Exception e) {
            log.error("撤销用户所有 Token 失败: {}", e.getMessage());
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

        try {
            String userTokensKey = RedisConstants.buildUserTokenListKey(userId);
            Set<Object> tokenIds = redisTemplate.opsForSet().members(userTokensKey);

            for (Object tokenId : tokenIds) {
                String tokenKey = RedisConstants.buildTokenKey(tokenId.toString());
                Map<Object, Object> tokenInfo = redisTemplate.opsForHash().entries(tokenKey);

                if (!tokenInfo.isEmpty()) {
                    Map<String, Object> tokenMap = new HashMap<>();
                    tokenInfo.forEach((key, value) -> tokenMap.put(key.toString(), value));
                    tokens.add(tokenMap);
                }
            }
        } catch (Exception e) {
            log.error("获取用户 Token 列表失败: {}", e.getMessage());
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

        try {
            Object onlineCount = redisTemplate.opsForValue().get(RedisConstants.ONLINE_STATS_KEY);
            stats.put("onlineCount", onlineCount != null ? Integer.parseInt(onlineCount.toString()) : 0);
            stats.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        } catch (Exception e) {
            log.error("获取在线统计失败: {}", e.getMessage());
            stats.put("onlineCount", 0);
        }

        return stats;
    }

    /**
     * 刷新令牌
     * @param oldTokenId
     * @return: java.util.Map<java.lang.String,java.lang.Object>
     */
    @Override
    public Map<String, Object> refreshToken(String oldTokenId) {
        try {
            String tokenKey = RedisConstants.buildTokenKey(oldTokenId);
            Map<Object, Object> tokenInfo = redisTemplate.opsForHash().entries(tokenKey);

            if (tokenInfo.isEmpty()) {
                return null;
            }

            Long userId = Long.valueOf(tokenInfo.get("userId").toString());
            String username = tokenInfo.get("username").toString();
            String deviceInfo = tokenInfo.get("deviceInfo").toString();

            // 撤销旧 Token
            revokeToken(oldTokenId);

            // 创建新 Token
            return createToken(userId, username, deviceInfo);
        } catch (Exception e) {
            log.error("刷新 Token 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 更新在线统计
     */
    private void updateOnlineStats() {
        try {
            // 统计所有 Token Key 的数量
            Set<String> tokenKeys = redisTemplate.keys(RedisConstants.TOKEN_PREFIX + "*");
            int onlineCount = tokenKeys != null ? tokenKeys.size() : 0;

            redisTemplate.opsForValue().set(RedisConstants.ONLINE_STATS_KEY, onlineCount, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("更新在线统计失败: {}", e.getMessage());
        }
    }

    /**
     * 更新 Token 最后访问时间
     * @param tokenId Token ID
     */
    @Override
    public void updateLastAccessTime(String tokenId) {
        try {
            String tokenKey = RedisConstants.buildTokenKey(tokenId);
            redisTemplate.opsForHash().put(tokenKey, "lastAccessTime",
                LocalDateTime.now().format(DATE_FORMATTER));
        } catch (Exception e) {
            log.error("更新访问时间失败: {}", e.getMessage());
        }
    }
}