package com.perch.service.impl;

import com.perch.pojo.entity.User;
import com.perch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户详情服务实现类（专门为 Spring Security 提供）
 */
@Slf4j
@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户信息: {}", username);

        try {
            // 调用业务层查询用户
            User user = userService.loadUserByUsername(username);

            // 检查用户状态
            if (user.getStatus() == 0) {
                throw new UsernameNotFoundException("用户已被禁用: " + user.getId());
            }

            // 构建权限列表
            String role = user.getRole();
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

            // 返回 Spring Security 的 User 对象
            return org.springframework.security.core.userdetails.User.builder()
                    .username(String.valueOf(user.getId()))
                    .password(user.getPasswordHash())
                    .authorities(Collections.singletonList(authority))
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false)
                    .build();

        } catch (UsernameNotFoundException e) {
            log.warn("用户不存在: {}", username);
            throw e;
        } catch (Exception e) {
            log.error("加载用户信息失败: {}", e.getMessage());
            throw new UsernameNotFoundException("用户加载失败: " + username);
        }
    }
}
