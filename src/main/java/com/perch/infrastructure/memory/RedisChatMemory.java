package com.perch.infrastructure.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.perch.constants.RedisConstants;
import com.perch.pojo.dto.request.MessageHolder;
import com.perch.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
/**
 * Redis上下文记忆管理
 */
@Slf4j
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = RedisConstants.AI_CHAT_PREFIX;

    private final RedisTemplate<String, Object> redisTemplate;
    private final int memorySize;

    public RedisChatMemory(RedisTemplate<String, Object> redisTemplate, int memorySize) {
        this.redisTemplate = redisTemplate;
        this.memorySize = memorySize;
    }

    @Override
    public void add(@NotNull String conversationId, @NotNull List<Message> messages) {
        String key = KEY_PREFIX + conversationId;

        // 转换为 DTO 列表
        List<MessageHolder> holders = messages.stream()
                .map(m -> new MessageHolder(m.getMessageType().name(), m.getText()))
                .collect(Collectors.toList());

        redisTemplate.opsForList().rightPushAll(key, holders.toArray());
        trim(key);
    }

    @NotNull
    @Override
    public List<Message> get(@NotNull String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<Object> rawList = redisTemplate.opsForList().range(key, -memorySize, -1);

        if (rawList == null) return new ArrayList<>();

        return rawList.stream()
                .map(obj -> (MessageHolder) obj)
                .map(h -> {
                    // 根据类型还原 Spring AI 的 Message 对象
                    if ("USER".equalsIgnoreCase(h.getType())) return new UserMessage(h.getContent());
                    if ("ASSISTANT".equalsIgnoreCase(h.getType())) return new AssistantMessage(h.getContent());
                    if ("SYSTEM".equalsIgnoreCase(h.getType())) return new SystemMessage(h.getContent());
                    return new UserMessage(h.getContent());
                })
                .collect(Collectors.toList());
    }

    @Override
    public void clear(@NotNull String conversationId) {
        redisTemplate.delete(KEY_PREFIX +conversationId);
    }

    private void trim(String key) {
        redisTemplate.opsForList().trim(key, -memorySize, -1);
    }
}
