package com.perch.service;

import com.perch.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     * @param user 用户信息
     * @return 注册结果
     */
    User register(User user);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户信息
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户信息
     */
    Optional<User> findById(Long id);

    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 更新结果
     */
    User update(User user);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 重置密码
     * @param email 邮箱
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean resetPassword(String email, String newPassword);

    /**
     * 删除用户
     * @param id 用户ID
     * @return 是否成功
     */
    boolean deleteById(Long id);

    /**
     * 查询所有用户
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 根据用户名查询用户（为 UserDetailsService 准备）
     * @param username 用户名
     * @return 用户信息
     */
    User loadUserByUsername(String username);
}