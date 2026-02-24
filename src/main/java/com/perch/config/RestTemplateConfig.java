package com.perch.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.perch.infrastructure.memory.RedisChatMemory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * 专门为 ChatMemory 定义的 RedisTemplate。
     * 使用 JDK 序列化是为了兼容 Spring AI 中 Message 接口及其各种子类的多态性。
     */
    @Bean
    public RedisTemplate<String, Object> chatMemoryRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 依然使用 String
        template.setKeySerializer(new StringRedisSerializer());

        // 使用 Jackson 序列化 Value
        ObjectMapper mapper = new ObjectMapper();
        // 关键配置：在 JSON 中包含类型信息，这样反序列化 Message 接口时才能找到子类
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        // 如果你有自定义的时间类，记得注册 JavaTimeModule
        mapper.registerModule(new JavaTimeModule());

        GenericJackson2JsonRedisSerializer jacksonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 构造 RedisChatMemory。
     * memorySize 建议从 application.yml 中读取，而不是硬编码。
     */
    @Bean
    public ChatMemory chatMemory(@Qualifier("chatMemoryRedisTemplate") RedisTemplate<String, Object> chatMemoryRedisTemplate) {
        // 建议：此处可以根据 AGENTS.md 的风格，通过 @Value("${app.chat.memory-size:20}") 注入
        return new RedisChatMemory(chatMemoryRedisTemplate, 20);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // 考虑到 AI 响应可能较慢，建议 readTimeout 稍微设长一点，或者针对不同服务区分配置
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}