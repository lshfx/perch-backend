package com.perch.infrastructure.advisor;

import com.perch.pojo.entity.ChatMessage;
import com.perch.service.ChatMessageService;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class PostgresStreamArchiveAdvisor implements StreamAdvisor {

    private final ChatMessageService chatMessageService;
    private final Executor taskExecutor;

    public PostgresStreamArchiveAdvisor(ChatMessageService chatMessageService, @Qualifier("archiveTaskExecutor") Executor taskExecutor) {
        this.chatMessageService = chatMessageService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    @NonNull
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        Object rawId = request.context().get("conversationId");
        if (rawId == null) {
            log.warn("Context missing conversationId, skip archive.");
            return chain.nextStream(request);
        }

        Long sessionId;
        try {
            String idStr = rawId.toString();
            // 兼容 "userId:sessionId" 格式
            if (idStr.contains(":")) {
                String[] parts = idStr.split(":");
                // 取冒号后面的部分作为 sessionId
                sessionId = Long.parseLong(parts[1]);
            } else {
                // 兼容纯数字格式
                sessionId = Long.parseLong(idStr);
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            log.error("Invalid conversationId format: {}", rawId);
            return chain.nextStream(request);
        }

        String userText = extractLastUserText(request);
        saveToDbAsync("user", userText, 0, sessionId);

        Flux<ChatClientResponse> responseFlux = chain.nextStream(request);
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        StringBuilder contentBuilder = new StringBuilder();

        return responseFlux.doOnNext(chatClientResponse -> {
            ChatResponse response = chatClientResponse.chatResponse();
            if (response != null && response.getResult() != null) {
                String text = response.getResult().getOutput().getText();
                if (text != null) {
                    contentBuilder.append(text);
                }
            }
            if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                usageRef.set(response.getMetadata().getUsage());
            }
        }).doOnComplete(() -> {
            String fullContent = contentBuilder.toString();
            if (StringUtils.hasText(fullContent)) {
                Usage finalUsage = usageRef.get();
                Integer tokenUsage = (finalUsage != null) ? finalUsage.getTotalTokens() : 0;
                saveToDbAsync("assistant", fullContent, tokenUsage, sessionId);
            }
        });
    }

    private String extractLastUserText(ChatClientRequest request) {
        if (request.prompt() == null || request.prompt().getUserMessages() == null || request.prompt().getUserMessages().isEmpty()) {
            return "";
        }
        UserMessage userMessage = request.prompt().getUserMessages().get(request.prompt().getUserMessages().size() - 1);
        return userMessage != null && userMessage.getText() != null ? userMessage.getText() : "";
    }

    private void saveToDbAsync(String role, String content, Integer tokenUsage, Long sessionId) {
        CompletableFuture.runAsync(() -> {
            try {
                ChatMessage msg = new ChatMessage();
                msg.setSessionId(sessionId);
                msg.setRole(role);
                msg.setContent(content);
                msg.setCreatedAt(LocalDateTime.now());
                msg.setTokenUsage(tokenUsage);
                msg.setMsgType("text");
                msg.setEmotionTag(null);
                chatMessageService.save(msg);
            } catch (Exception e) {
                log.error("Archive failed: {}", e.getMessage());
            }
        }, taskExecutor);
    }

    @Override
    @NonNull
    public String getName() {
        return "PostgresStreamArchiveAdvisor";
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
