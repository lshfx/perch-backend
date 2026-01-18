package com.perch.filter;

import com.perch.service.TokenService;
import com.perch.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        try {
            // 从请求头中获取 JWT Token
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 解析 Token 获取 Claims
                Claims claims = jwtUtils.getAllClaimsFromToken(token);

                if (claims != null) {
                    String tokenId = (String) claims.get("tokenId");
                    String username = claims.getSubject();

                    // 双重验证：1. JWT 本身有效性 2. Redis 白名单验证
                    if (tokenId != null &&
                        jwtUtils.validateToken(token, username) &&
                        tokenService.validateToken(tokenId)) {

                        // 验证用户名是否有效
                        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                            // 加载用户详情
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                            // 创建认证对象
                            UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                                );

                            // 设置认证详情
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            // 将认证信息存入 SecurityContext
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            // 更新 Token 最后访问时间
                            Object userIdObj = claims.get("userId");
                            Long userId = null;
                            if (userIdObj instanceof Number) {
                                userId = ((Number) userIdObj).longValue();
                            } else if (userIdObj != null) {
                                try {
                                    userId = Long.valueOf(userIdObj.toString());
                                } catch (NumberFormatException ignored) {
                                }
                            }
                            tokenService.updateLastAccessTime(tokenId, userId);

                            log.debug("用户 {} 认证成功，Token: {}", username, tokenId);
                        }
                    } else {
                        log.warn("Token 验证失败: tokenId={}, username={}", tokenId, username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("无法设置用户认证: {}", e.getMessage());
            // 清除认证信息
            SecurityContextHolder.clearContext();
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取 JWT Token
     * @param request HTTP 请求
     * @return JWT Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtUtils.getHeaderName());

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtUtils.getTokenPrefix() + " ")) {
            return bearerToken.substring(jwtUtils.getTokenPrefix().length() + 1);
        }

        return null;
    }

    /**
     * 判断是否应该跳过此过滤器
     * @param request HTTP 请求
     * @return 是否跳过
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip public auth endpoints except logout and /me (needs JWT context)
        if (path.startsWith("/api/auth/")
                && !path.startsWith("/api/auth/logout")
                && !path.startsWith("/api/auth/me")) {
            return true;
        }

        return path.startsWith("/api/ping") ||
               path.startsWith("/api/ai/chat") ||
               path.startsWith("/public/") ||
               path.startsWith("/error") ||
               path.startsWith("/actuator/");
    }
}
