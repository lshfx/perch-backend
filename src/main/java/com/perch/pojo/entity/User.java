package com.perch.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("users")
public class User {

    /**
     * 用户ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 邮箱（唯一，不能为空）
     */
    @TableField("email")
    private String email;

    /**
     * 密码哈希（不能为空）
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 微信 OpenID (小程序/App 唯一标识)
     */
    private String wechatOpenid;

    /**
     * 微信 UnionID (跨应用唯一标识，如同时有公众号和小程序)
     */
    private String wechatUnionid;

    /**
     * 昵称（默认为'用户'）
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 用户角色（默认为'USER'）
     */
    @TableField("role")
    private String role;

    /**
     * 用户状态（默认为1，1-正常，0-禁用）
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间（默认为当前时间）
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间（默认为当前时间）
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}