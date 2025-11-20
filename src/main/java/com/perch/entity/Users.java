package com.perch.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (Users)表实体类
 *
 * @author lsh
 * @since 2025-11-20 13:51:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Users{

    private Long id;

    private String email;

    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    private String role;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

