package com.perch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perch.entity.User;
import com.perch.mapper.UserMapper;
import com.perch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(User user) {
        try {
            // 1. 检查用户名和邮箱是否已存在
            if (existsByUsername(user.getUsername())) {
                throw new RuntimeException("用户名已存在");
            }
            if (existsByEmail(user.getEmail())) {
                throw new RuntimeException("邮箱已被注册");
            }

            // 2. 加密密码
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            // 3. 设置默认值
            user.setStatus(1); // 正常状态
            user.setRole("USER"); // 默认角色
            user.setDeleted(0); // 未删除

            // 4. 保存用户
            userMapper.insert(user);

            log.info("用户 {} 注册成功", user.getUsername());
            return user;

        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage());
            throw new RuntimeException("注册失败: " + e.getMessage());
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username)
               .eq("deleted", 0);

        User user = userMapper.selectOne(wrapper);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email)
               .eq("deleted", 0);

        User user = userMapper.selectOne(wrapper);
        return Optional.ofNullable(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username)
               .eq("deleted", 0);

        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email)
               .eq("deleted", 0);

        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Optional<User> findById(Long id) {
        User user = userMapper.selectById(id);
        return Optional.ofNullable(user);
    }

    @Override
    @Transactional
    public User update(User user) {
        userMapper.updateById(user);
        return user;
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        try {
            Optional<User> userOpt = findById(userId);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("用户不存在");
            }

            User user = userOpt.get();

            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return false;
            }

            // 更新新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);

            return true;

        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean resetPassword(String email, String newPassword) {
        try {
            Optional<User> userOpt = findByEmail(email);
            if (!userOpt.isPresent()) {
                return false;
            }

            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);

            return true;

        } catch (Exception e) {
            log.error("重置密码失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public List<User> findAll() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        return userMapper.selectList(wrapper);
    }

    @Override
    public User loadUserByUsername(String username) {
        Optional<User> userOpt = findByUsername(username);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("用户不存在: " + username);
        }
        return userOpt.get();
    }
}