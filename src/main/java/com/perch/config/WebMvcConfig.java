package com.perch.config;

import com.perch.infrastructure.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 将限流拦截器专门挂载到 AI 聊天流式接口上
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/ai/stream"); // 只拦截发消息的接口
    }

    /**
     * 配置 Spring MVC 异步请求的线程池 (消除 SimpleAsyncTaskExecutor 警告)
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mvc-async-");
        // 拒绝策略：直接由 Tomcat 的 NIO 线程来执行（防止丢消息）
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        // 将自定义线程池交给 Spring MVC
        configurer.setTaskExecutor(executor);
        
        // 可选：设置异步请求的全局超时时间（例如 60 秒）
        configurer.setDefaultTimeout(60000L); 
    }
}