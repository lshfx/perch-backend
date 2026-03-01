package com.perch.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_persona_tags")
public class UserPersonaTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long sessionId;
    
    private String tagName;
    private String reasoning;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}