package com.perch.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (ChatSession)表实体类
 *
 * @author lsh
 * @since 2025-11-20 13:51:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("chat_sessions")
public class ChatSession {

    private Long id;

    private Long userId;

    private String title;

    private String advisorType;

    private Boolean isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}