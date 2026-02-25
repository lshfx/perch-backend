package com.perch.service;

import reactor.core.publisher.Flux;

/**
 * @Author: lsh
 * @Date: 2026/01/25/9:23
 * @Description: Ai聊天服务类
 */
public interface AiChatService{
    Flux<String> streamChat(String message, Long sessionId);

}
