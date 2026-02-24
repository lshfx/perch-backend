package com.perch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: lsh
 * @Date: 2026/01/28/11:50
 * @Description: 线程池配置
 */
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "archiveTaskExecutor")
    public Executor archiveTaskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据你的 CPU 核数调整，IO 密集型通常设为 CPU * 2
        executor.setCorePoolSize(4);
        // 最大线程数：突发流量时的最大承载
        executor.setMaxPoolSize(16);
        // 队列大小：允许积压多少条日志等待写入
        executor.setQueueCapacity(500);
        // 线程前缀：方便查日志和监控
        executor.setThreadNamePrefix("chat-archive-");
        // 拒绝策略：如果队列满了，由调用者线程自己执行（降级策略，保证不丢数据但会变慢）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅停机：应用关闭时等待任务执行完
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

}
