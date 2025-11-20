package com.perch.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * -- GETTER --
     *  获取 Token 过期时间（毫秒）
     *
     * @return 过期时间
     */
    @Getter
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * -- GETTER --
     *  获取刷新 Token 过期时间（毫秒）
     *
     * @return 刷新 Token 过期时间
     */
    @Getter
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * -- GETTER --
     *  获取 Token 前缀
     *
     * @return Token Prefix
     */
    @Getter
    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    /**
     * -- GETTER --
     *  获取 Header 名称
     *
     * @return Header Name
     */
    @Getter
    @Value("${jwt.header-name}")
    private String headerName;

    /**
     * 获取密钥
     * @return SecretKey
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 Token
     * @param username 用户名
     * @return JWT Token
     */
    public String generateToken(String username) {
        return generateToken(username, new HashMap<>());
    }

    /**
     * 生成 Token（带自定义声明）
     * @param username 用户名
     * @param claims 自定义声明
     * @return JWT Token
     */
    public String generateToken(String username, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 生成刷新 Token
     * @param username 用户名
     * @return 刷新 Token
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 从 Token 中获取用户名
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从 Token 中获取过期时间
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 从 Token 中获取 Claims
     * @param token JWT Token
     * @return Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否过期
     * @param token JWT Token
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            return !expiration.before(new Date());
        } catch (JwtException e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证 Token 是否有效
     * @param token JWT Token
     * @param username 用户名
     * @return 是否有效
     */
    public boolean validateToken(String token, String username) {
        try {
            String tokenUsername = getUsernameFromToken(token);
            return tokenUsername.equals(username) && isTokenExpired(token);
        } catch (JwtException e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从请求头中获取 Token
     * @param authHeader Authorization Header
     * @return JWT Token
     */
    public String getTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith(tokenPrefix + " ")) {
            return authHeader.substring(tokenPrefix.length() + 1);
        }
        return null;
    }

    /**
     * 刷新 Token
     * @param refreshToken 刷新 Token
     * @return 新的访问 Token
     */
    public String refreshToken(String refreshToken) {
        try {
            String username = getUsernameFromToken(refreshToken);
            if (username != null && isTokenExpired(refreshToken)) {
                return generateToken(username);
            }
        } catch (JwtException e) {
            log.error("刷新 Token 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 解析 Token 获取所有 Claims
     * @param token JWT Token
     * @return Claims
     */
    public Claims getAllClaimsFromToken(String token) {
        try {
            return getClaimsFromToken(token);
        } catch (JwtException e) {
            log.error("解析 Token 失败: {}", e.getMessage());
            return null;
        }
    }
}