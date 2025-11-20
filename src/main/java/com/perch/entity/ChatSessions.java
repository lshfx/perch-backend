package com.perch.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (ChatSessions)表实体类
 *
 * @author lsh
 * @since 2025-11-20 13:51:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatSessions{

    private Long id;

    private Long userId;

    private String title;

    private String advisorType;

    private Boolean isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

