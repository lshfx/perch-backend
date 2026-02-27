package com.perch.controller;

import com.perch.pojo.common.Result;
import com.perch.pojo.entity.ChatSession;
import com.perch.security.SecurityUtils;
import com.perch.service.AiChatService;
import com.perch.service.ChatSessionService;
import com.perch.service.EmotionAnalysisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 聊天控制器
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {


    private final AiChatService aiChatService;

    private final ChatSessionService chatSessionService;

    /**
     * AI 聊天接口
     * @param message 用户消息
     * @param sessionId 会话ID（可选，为空则新建）
     * @return 包含 Header 和流式 Body 的 ResponseEntity
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // 👈 明确声明这是一个 SSE 流
    public ResponseEntity<Flux<String>> streamChat(
            @RequestParam String message,
            @RequestParam(required = false) Long sessionId
    ) {
        // 1. 如果是第一次对话，先创建会话ID
        if (sessionId == null) {
            ChatSession session = new ChatSession();
            session.setUserId(SecurityUtils.getCurrentUserId());
            session.setTitle(message.length() > 10 ? message.substring(0, 10) : message);
            chatSessionService.save(session);
            sessionId = session.getId();
        }

        // 2. 获取 AI 返回的流
        Flux<String> aiResponseStream = aiChatService.streamChat(message, sessionId);

        // 3. 使用 ResponseEntity 组装 Header 和 Flux 响应体返回
        return ResponseEntity.ok()
                .header("X-Session-Id", String.valueOf(sessionId)) // 👈 优雅地塞入 Header
                .body(aiResponseStream);
    }
}