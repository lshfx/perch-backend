package com.perch.service.impl;

import com.perch.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @Author: lsh
 * @Date: 2026/01/25/9:26
 * @Description: AI聊天服务实现类
 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService{

    private final ChatClient chatClient; // 注入装配好的终极体

    @Override
    public Flux<String> streamChat(String userMessage, Long sessionId) {

        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", String.valueOf(sessionId))
                        .param("conversationId",String.valueOf(sessionId)))
                .toolContext(Map.of("sessionId", sessionId))
                .stream()
                .content();
    }
}
