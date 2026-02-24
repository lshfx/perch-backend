package com.perch.config;

import com.perch.infrastructure.advisor.PostgresStreamArchiveAdvisor;
import com.perch.infrastructure.memory.RedisChatMemory;
import com.perch.service.ChatMessageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Executor;

@Configuration
public class AiConfig {

    /**
     * 这里不再需要手动定义 OpenAiChatModel 或 DeepSeekChatModel。
     * Spring AI 的自动配置（Auto-configuration）会根据 application.yml 中的配置
     * 自动创建一个 ChatModel Bean，并注入到这里的参数中。
     */
    @Bean
    public ChatClient chatClient(
            @Qualifier("deepSeekChatModel") ChatModel chatModel, // Spring 自动注入配置好的 DeepSeekChatModel
            ChatMemory chatMemory,
            PostgresStreamArchiveAdvisor postgresAdvisor) {

        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个乐于助人的 AI 助手，请用中文回答。")
                // 配置上下文记忆 (Redis)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 配置你的自定义归档逻辑 (Postgres)
                .defaultAdvisors(postgresAdvisor)
                .build();
    }

    // 保留你的自定义 Advisor 逻辑
    @Bean
    public PostgresStreamArchiveAdvisor postgresAdvisor(
            ChatMessageService chatMessageService,
            @Qualifier("archiveTaskExecutor") Executor executor) {
        return new PostgresStreamArchiveAdvisor(chatMessageService, executor);
    }

    // 保留 Redis 聊天记忆配置
    @Bean
    public ChatMemory chatMemory(RedisTemplate<String, Object> redisTemplate) {
        // 这里的 20 是指保留最近 20 条消息上下文，可根据 DeepSeek 的 context window 调整
        return new RedisChatMemory(redisTemplate, 20);
    }
}
