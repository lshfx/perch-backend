package com.perch.config;

import com.perch.filter.JwtAuthenticationFilter;
import com.perch.infrastructure.interceptor.RateLimitInterceptor;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    /**
     * 安全过滤器链配置
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 禁用 CSRF（适用于 API 开发）
        http.csrf(AbstractHttpConfigurer::disable)

                // 配置会话管理为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // 允许匿名访问的接口
                        .requestMatchers("/api/ping").permitAll()
                        .requestMatchers("/api/auth/logout", "/api/auth/me").authenticated()
                        // 认证相关接口（登录、登出、刷新Token等）
                        .requestMatchers("/api/auth/**").permitAll()
                        // 管理员接口需要ADMIN角色
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 公开接口
                        .requestMatchers("/public/**").permitAll()
                        // 错误页面
                        .requestMatchers("/error/**").permitAll()
                        // 监控端点（开发环境）
                        .requestMatchers("/actuator/**").permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )

                // 添加 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}
