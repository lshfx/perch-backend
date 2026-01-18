package com.perch.service;

import java.util.List;
import java.util.Map;

/**
 * Token 管理服务
 */
public interface TokenService {

    /**
     * 创建并存储 Token
     * @param userId 用户ID
     * @param username 用户名
     * @param deviceInfo 设备信息
     * @return Token 信息
     */
    Map<String, Object> createToken(Long userId, String username, String deviceInfo);

    /**
     * 验证 Token 是否有效
     * @param tokenId Token ID
     * @return 是否有效
     */
    boolean validateToken(String tokenId);

    /**
     * 撤销 Token（用户登出）
     * @param tokenId Token ID
     */
    void revokeToken(String tokenId);

    /**
     * 撤销用户的所有 Token（强制下线）
     * @param userId 用户ID
     */
    void revokeAllUserTokens(Long userId);

    /**
     * 获取用户的所有在线 Token
     * @param userId 用户ID
     * @return Token 信息列表
     */
    List<Map<String, Object>> getUserTokens(Long userId);

    /**
     * 获取在线用户统计
     * @return 统计信息
     */
    Map<String, Object> getOnlineStats();

    /**
     * 刷新 Token
     * @param oldTokenId 旧 Token ID
     * @return 新 Token 信息
     */
    Map<String, Object> refreshToken(String oldTokenId, String refreshToken);

    /**
     * 校验 Token 并返回结果
     * @param tokenId Token ID
     * @return 校验结果
     */
    Map<String, Object> validateTokenInfo(String tokenId);

    /**
     * 登出当前 Token
     * @param token Token 字符串
     */
    void logout(String token);

    /**
     * 更新 Token 最后访问时间
     * @param tokenId Token ID
     */
    void updateLastAccessTime(String tokenId, Long userId);
}
