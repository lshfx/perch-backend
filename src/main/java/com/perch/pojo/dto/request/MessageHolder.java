package com.perch.pojo.dto.request;

import lombok.Data;

@Data
public class MessageHolder {
    private String type; // USER, ASSISTANT, SYSTEM
    private String content;

    public MessageHolder() {} // 必须有无参构造

    public MessageHolder(String type, String content) {
        this.type = type;
        this.content = content;
    }
    // Getter 和 Setter 省略...
}