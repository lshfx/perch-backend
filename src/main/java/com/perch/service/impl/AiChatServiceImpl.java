package com.perch.service.impl;

import com.perch.security.SecurityUtils;
import com.perch.service.AiChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @Author: lsh
 * @Date: 2026/01/25/9:26
 * @Description: AI聊天服务实现类
 */
@Service
public class AiChatServiceImpl implements AiChatService{

    // 直接注入成品 ChatClient，而不是 Builder
    private final ChatClient chatClient;

    public AiChatServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Flux<String> streamChat(String message, Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();

        String memoryId = userId + ":" + sessionId;

        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param("conversationId", memoryId))
                .stream()
                .content();
    }
}
