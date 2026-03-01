package com.perch.infrastructure.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MemoryCompressionAdvisor implements CallAdvisor, StreamAdvisor {

    private final ChatMemory chatMemory;
    private final ChatModel localModel;
    
    // 触发压缩的阈值：一问一答算2条，12条代表已经聊了6个回合
    private static final int MAX_WINDOW_SIZE = 12; 

    // 注入 Redis 记忆库和本地的 Ollama 小模型
    public MemoryCompressionAdvisor(ChatMemory chatMemory, @Qualifier("ollamaChatModel") ChatModel localModel) {
        this.chatMemory = chatMemory;
        this.localModel = localModel;
    }

    @Override
    public String getName() {
        return "MemoryCompressionAdvisor";
    }

    @Override
    public int getOrder() {
        // 关键：设为 -5，确保它在官方的 MessageChatMemoryAdvisor (0) 之前执行！
        return -5; 
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        compressIfNeeded(request);
        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        compressIfNeeded(request);
        return chain.nextStream(request);
    }

    /**
     * 核心压缩逻辑
     */
    /**
     * 核心压缩逻辑
     */
    private void compressIfNeeded(ChatClientRequest request) {
        Map<String, Object> context = request.context();
        String conversationId = null;

        // 🎯 终极防弹方案：直接使用源码底层的字符串常量，无视类路径变更
        String ADVISOR_PARAMS_KEY = "spring.ai.chat.client.advisor.params";
        String MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

        // 1. 尝试从外层直接提取
        if (context.containsKey("sessionId")) {
            conversationId = String.valueOf(context.get("sessionId"));
        } else if (context.containsKey(MEMORY_CONVERSATION_ID_KEY)) {
            conversationId = String.valueOf(context.get(MEMORY_CONVERSATION_ID_KEY));
        }
        // 2. 尝试从新版 API 的 Advisor 专属套娃 Map 中提取
        else if (context.containsKey(ADVISOR_PARAMS_KEY)) {
            Map<String, Object> advisorParams = (Map<String, Object>) context.get(ADVISOR_PARAMS_KEY);
            if (advisorParams != null) {
                if (advisorParams.containsKey("sessionId")) {
                    conversationId = String.valueOf(advisorParams.get("sessionId"));
                } else if (advisorParams.containsKey(MEMORY_CONVERSATION_ID_KEY)) {
                    conversationId = String.valueOf(advisorParams.get(MEMORY_CONVERSATION_ID_KEY));
                }
            }
        }

        // 如果依然找不到，打印出真实的 Keys 供我们排查
        if (conversationId == null) {
            System.out.println("⚠️ [记忆压缩] 未找到 conversationId！当前真实的 Context Keys: " + context.keySet());
            return;
        }

        // 3. 获取当前 Redis 里的完整历史
        List<Message> history = chatMemory.get(conversationId);

        // 4. 触发阈值判断（使用 >= ，确保第 7 回合刚好达到 12 条时立刻触发）
        if (history.size() >= MAX_WINDOW_SIZE) {
            System.out.println("🗜️ [记忆压缩] 触发上下文摘要，当前历史长度：" + history.size());

            // 取出最老的 6 条旧消息进行压缩
            List<Message> messagesToSummarize = history.subList(0, 6);
            List<Message> messagesToKeep = history.subList(6, history.size());

            String conversationText = messagesToSummarize.stream()
                    .map(m -> m.getMessageType().name() + ": " + m.getText())
                    .collect(Collectors.joining("\n"));

            String summaryPrompt = "请将以下多轮对话总结成一段100字以内的摘要，保留用户遇到的关键事件和核心情绪，不要废话：\n\n" + conversationText;

            try {
                // 调用本地 Ollama 进行静默摘要
                String summary = localModel.call(summaryPrompt);
                System.out.println("✅ [记忆压缩] 本地小模型摘要完成：" + summary);

                // 构造新的压缩版历史
                List<Message> newHistory = new ArrayList<>();
                newHistory.add(new SystemMessage("以下是较早之前的对话摘要：" + summary));
                newHistory.addAll(messagesToKeep);

                // 覆盖 Redis 中的老旧长对话
                chatMemory.clear(conversationId);
                chatMemory.add(conversationId, newHistory);

            } catch (Exception e) {
                System.err.println("❌ 记忆压缩失败，跳过此次压缩：" + e.getMessage());
            }
        }
    }
}