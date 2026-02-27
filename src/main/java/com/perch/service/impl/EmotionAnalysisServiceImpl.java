package com.perch.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.perch.infrastructure.schema.EmotionSchema;
import com.perch.mapper.ChatMessageMapper;
import com.perch.pojo.entity.ChatMessage;
import com.perch.service.EmotionAnalysisService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @Author: lsh
 * @Date: 2026/02/26/20:52
 * @Description:
 */
@Service
public class EmotionAnalysisServiceImpl implements EmotionAnalysisService {

    private final ChatModel localModel; // 直接注入 ChatModel，因为我们要每次构建特定的 Prompt
    private final ChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public EmotionAnalysisServiceImpl(
            // 这里的名字对应上面 @Bean 的方法名
            @Qualifier("ollamaChatModel") ChatModel localModel,
            ChatMessageMapper messageMapper,
            ObjectMapper objectMapper) {
        this.localModel = localModel;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    @Async("archiveTaskExecutor")
    public void analyzeAndSaveEmotionAsync(Long messageId, String userContent) {
        try {
            // 🎯 核心修改 1：泛型和 Class 对象都指向 EmotionSchema.EmotionTagResult
            BeanOutputConverter<EmotionSchema.EmotionTagResult> outputConverter =
                    new BeanOutputConverter<>(EmotionSchema.EmotionTagResult.class);

            Prompt prompt = new Prompt(
                    "请提取以下用户发言的核心情绪和强度。用户发言：\"" + userContent + "\"",
                    OllamaChatOptions.builder()
                            .format(outputConverter.getJsonSchemaMap())
                            .build()
            );

            String rawJsonContent = this.localModel.call(prompt).getResult().getOutput().getText();

            // 🎯 核心修改 2：接收的变量类型也要对应上
            EmotionSchema.EmotionTagResult result = outputConverter.convert(rawJsonContent);

            if (result != null && result.type() != null) {
                String tagJson = objectMapper.writeValueAsString(result);

                messageMapper.update(null, Wrappers.<ChatMessage>lambdaUpdate()
                        .set(ChatMessage::getEmotionTag, tagJson)
                        .eq(ChatMessage::getId, messageId)
                );
                System.out.println("✅ [Ollama枚举提取] 消息ID " + messageId + " -> " + tagJson);
            }

        } catch (Exception e) {
            System.err.println("❌ 本地情绪提取失败，消息ID: " + messageId + "，原因: " + e.getMessage());
        }
    }
}
