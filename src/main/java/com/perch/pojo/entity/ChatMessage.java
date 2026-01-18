package com.perch.pojo.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (ChatMessage)表实体类
 *
 * @author lsh
 * @since 2025-11-20 13:51:16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {

    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    private String msgType;

    private Integer tokenUsage;

    private String emotionTag;

    private LocalDateTime createdAt;
}