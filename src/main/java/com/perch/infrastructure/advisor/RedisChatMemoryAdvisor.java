package com.perch.infrastructure.advisor;

import static com.perch.constants.RedisConstants.AI_CHAT_PREFIX;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class RedisChatMemoryAdvisor implements StreamAdvisor {

    private final RedisTemplate<String, Object> redisTemplate;
    private final int memorySize;

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String conversationId = (String) request.context().get("conversationId");
        if (!StringUtils.hasText(conversationId) || memorySize <= 0) {
            return chain.nextStream(request);
        }

        String redisKey = AI_CHAT_PREFIX + conversationId;

        List<Object> rangeHistory = redisTemplate.opsForList().range(redisKey, -memorySize, -1);
        List<Message> historyMessages = new ArrayList<>();
        if (rangeHistory != null) {
            historyMessages.addAll(rangeHistory.stream()
                    .filter(obj -> obj instanceof Message)
                    .map(Message.class::cast)
                    .toList());
        }

        List<Message> newMessages = new ArrayList<>(historyMessages);
        List<Message> requestMessages = request.prompt().getInstructions();
        Message lastHistoryMessage = historyMessages.isEmpty() ? null : historyMessages.get(historyMessages.size() - 1);
        if (!requestMessages.isEmpty() && lastHistoryMessage != null && lastHistoryMessage.equals(requestMessages.get(0))) {
            newMessages.addAll(requestMessages.subList(1, requestMessages.size()));
        } else {
            newMessages.addAll(requestMessages);
        }

        Prompt newPrompt = new Prompt(newMessages, request.prompt().getOptions());
        ChatClientRequest newRequest = request.mutate().prompt(newPrompt).build();
        Flux<ChatClientResponse> responseFlux = chain.nextStream(newRequest);

        UserMessage currentUserMsg = request.prompt().getUserMessage();
        if (currentUserMsg != null) {
            redisTemplate.opsForList().rightPush(redisKey, currentUserMsg);
        }

        StringBuilder contentBuilder = new StringBuilder();
        return responseFlux.doOnNext(chatClientResponse -> {
            ChatResponse response = chatClientResponse.chatResponse();
            if (response != null && response.getResult() != null) {
                String text = response.getResult().getOutput().getText();
                if (text != null) {
                    contentBuilder.append(text);
                }
            }
        }).doOnComplete(() -> {
            String assistantText = contentBuilder.toString();
            if (!assistantText.isEmpty()) {
                redisTemplate.opsForList().rightPush(redisKey, new AssistantMessage(assistantText));
            }
        }).doFinally(signalType -> redisTemplate.opsForList().trim(redisKey, -memorySize, -1));
    }

    @Override
    public String getName() {
        return "RedisChatMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
