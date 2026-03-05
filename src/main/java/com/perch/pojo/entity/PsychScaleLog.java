package com.perch.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("psych_scale_logs")
public class PsychScaleLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private String scaleName;
    private Integer score;
    private String severity;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}