package com.perch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * @Author: lsh
 * @Date: 2026/01/31/9:23
 * @Description: WebSocket配置类
 */

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 设定连接端点，前端连接 ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 允许跨域
                .withSockJS(); // 备选方案，支持不支持 WS 的浏览器
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 2. 设定消息代理
        // 前端订阅 "/user/queue/..." 接收私人消息
        registry.enableSimpleBroker("/queue", "/topic");
        // 前端发送消息给后端时，前缀是 "/app"
        registry.setApplicationDestinationPrefixes("/app");
        // 开启点对点消息支持 (关键：为了能用 convertAndSendToUser)
        registry.setUserDestinationPrefix("/user");
    }
}
