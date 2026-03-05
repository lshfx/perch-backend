package com.perch.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dream_logs")
public class DreamLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private String dreamElement;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}