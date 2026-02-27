package com.perch.infrastructure.schema;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class EmotionSchema {

    // 1. 定义一个你的树洞系统专属的“情绪分类法”
    public enum EmotionType {
        焦虑, 抑郁, 愤怒, 悲伤, 孤独, 恐惧, 委屈, 疲惫, 平静, 喜悦, 未知
    }

    // 2. 在 Record 中直接使用该枚举
    public record EmotionTagResult(
            @JsonPropertyDescription("必须从给定的枚举列表中选择最贴切的一个核心情绪。")
            EmotionType type,

            @JsonPropertyDescription("情绪的强烈程度，1-10的整数")
            Integer score
    ) {}
}