package com.perch.controller;

import com.perch.entity.common.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 聊天控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final ChatClient chatClient;

    /**
     * 构造函数注入 ChatClient.Builder
     * @param chatClientBuilder ChatClient 构建器
     */
    @Autowired
    public AiChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * AI 聊天接口
     * @param message 用户消息
     * @return AI 回答
     */
    @GetMapping("/chat")
    public Result<String> chat(@RequestParam String message) {
        try {
            // 调用 AI 模型并返回回答
            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            return Result.success(response, "AI 回答成功");
        } catch (Exception e) {
            return Result.error("AI 服务异常: " + e.getMessage());
        }
    }
}