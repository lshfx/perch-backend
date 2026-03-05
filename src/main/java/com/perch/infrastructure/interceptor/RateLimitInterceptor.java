package com.perch.infrastructure.interceptor;

import com.perch.constants.RedisConstants;
import com.perch.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import static com.perch.constants.RedisConstants.RATE_LIMIT_PREFIX;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    // 限流配置：每 10 秒最多 3 次请求（可根据需要调整）
    private static final int MAX_REQUESTS = 3;
    private static final int TIME_WINDOW_SECONDS = 10;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 🎯 关键修改：直接使用你的 SecurityUtils 从安全上下文中提取真实 userId
        Long userId = SecurityUtils.getCurrentUserId();

        // 如果未登录，降级使用 IP 地址作为限流标识
        String clientIdentifier = (userId != null) ? "user:" + userId : "ip:" + request.getRemoteAddr();

        // 构造 Redis Key
        String redisKey = RedisConstants.buildRateLimitKey("ai_chat:" + clientIdentifier);

        try {
            // 利用 Redis 原子操作计数
            Long currentCount = redisTemplate.opsForValue().increment(redisKey, 1);

            // 第一次访问时开启时间窗口
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(redisKey, TIME_WINDOW_SECONDS, TimeUnit.SECONDS);
            }

            // 触发限流
            if (currentCount != null && currentCount > MAX_REQUESTS) {
                System.out.println("🚨 [安全防线] 触发限流！标识: " + clientIdentifier + " 请求过于频繁！");

                // 返回 429 状态码
                response.setStatus(429);
                response.setContentType("text/plain;charset=UTF-8");
                PrintWriter writer = response.getWriter();
                writer.write("您说话太快啦，请稍微深呼吸一下，喝口水再接着聊吧~");
                writer.flush();
                return false; // 阻断请求
            }
        } catch (Exception e) {
            // Redis 宕机时放行（高可用降级）
            System.err.println("Redis 限流器异常，自动降级放行：" + e.getMessage());
        }

        return true;
    }
}