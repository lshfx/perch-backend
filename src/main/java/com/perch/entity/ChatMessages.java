package com.perch.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (ChatMessages)表实体类
 *
 * @author lsh
 * @since 2025-11-20 13:51:16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessages{

    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    private String msgType;

    private Integer tokenUsage;

    private String emotionTag;

    private LocalDateTime createdAt;
}

