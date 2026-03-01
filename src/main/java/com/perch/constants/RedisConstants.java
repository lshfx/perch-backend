package com.perch.constants;

/**
 * Redis Key 常量类
 */
public class RedisConstants {

    /**
     * 防止实例化
     */
    private RedisConstants() {}

    // ==================== 认证相关 ====================

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "login:token:";

    /**
     * 用户 Token 列表前缀
     */
    public static final String USER_PREFIX = "login:user:";

    /***
     * 验证码前缀
     */
    public static final String VERIFY_CODE_PREFIX = "verify:email:";


    /**
     * 在线统计 Key
     */
    public static final String ONLINE_STATS_KEY = "login:stats:online";

    // ==================== 缓存相关 ====================

    /**
     * 用户缓存前缀
     */
    public static final String USER_CACHE_PREFIX = "cache:user:";

    /**
     * 角色缓存前缀
     */
    public static final String ROLE_CACHE_PREFIX = "cache:role:";

    /**
     * 权限缓存前缀
     */
    public static final String PERMISSION_CACHE_PREFIX = "cache:permission:";

    // ==================== 会话相关 ====================

    /**
     * 会话前缀
     */
    public static final String SESSION_PREFIX = "session:";

    /**
     * 验证码前缀
     */
    public static final String CAPTCHA_PREFIX = "captcha:";

    // ==================== 业务相关 ====================

    /**
     * AI 对话历史前缀
     */
    public static final String AI_CHAT_PREFIX = "chat:history:";

    /**
     * 用户偏好设置前缀
     */
    public static final String USER_PREFERENCES_PREFIX = "user:preferences:";

    /**
     * 心理评估结果前缀
     */
    public static final String PSYCHO_ASSESSMENT_PREFIX = "psycho:assessment:";

    // ==================== 系统相关 ====================

    /**
     * 系统配置前缀
     */
    public static final String CONFIG_PREFIX = "config:";

    /**
     * 字典数据前缀
     */
    public static final String DICT_PREFIX = "dict:";

    /**
     * 分布式锁前缀
     */
    public static final String LOCK_PREFIX = "lock:";

    // ==================== 限流相关 ====================

    /**
     * 接口限流前缀
     */
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * 用户操作限流前缀
     */
    public static final String USER_RATE_LIMIT_PREFIX = "user_rate_limit:";

    // ==================== 消息队列相关 ====================

    /**
     * 消息队列前缀
     */
    public static final String MQ_PREFIX = "mq:";

    /**
     * 任务队列前缀
     */
    public static final String TASK_QUEUE_PREFIX = "task:queue:";

    // ==================== 日志相关 ====================

    /**
     * 操作日志前缀
     */
    public static final String OPERATION_LOG_PREFIX = "log:operation:";

    /**
     * 错误日志前缀
     */
    public static final String ERROR_LOG_PREFIX = "log:error:";

    // ==================== 统计相关 ====================

    /**
     * 访问统计前缀
     */
    public static final String ACCESS_STATS_PREFIX = "stats:access:";

    /**
     * 用户活跃统计前缀
     */
    public static final String USER_ACTIVITY_PREFIX = "stats:activity:";

    // ==================== 文件相关 ====================

    /**
     * 文件上传临时前缀
     */
    public static final String UPLOAD_TEMP_PREFIX = "upload:temp:";

    /**
     * 文件元数据前缀
     */
    public static final String FILE_METADATA_PREFIX = "file:metadata:";

    // ==================== 通知相关 ====================

    /**
     * 通知前缀
     */
    public static final String NOTIFICATION_PREFIX = "notification:";

    /**
     * 用户未读通知前缀
     */
    public static final String USER_UNREAD_PREFIX = "user:unread:";

    // ==================== 工具方法 ====================

    /**
     * 构建用户缓存 Key
     * @param userId 用户ID
     * @return Key
     */
    public static String buildUserCacheKey(Long userId) {
        return USER_CACHE_PREFIX + userId;
    }

    /**
     * 构建 Token Key
     * @param tokenId Token ID
     * @return Key
     */
    public static String buildTokenKey(String tokenId) {
        return TOKEN_PREFIX + tokenId;
    }

    /**
     * 构建用户 Token 列表 Key
     * @param userId 用户ID
     * @return Key
     */
    public static String buildUserTokenListKey(Long userId) {
        return USER_PREFIX + userId;
    }

    /**
     * 构建 AI 对话历史 Key
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return Key
     */
    public static String buildAiChatKey(Long userId, String sessionId) {
        return AI_CHAT_PREFIX + userId + ":" + sessionId;
    }

    /**
     * 构建用户偏好设置 Key
     * @param userId 用户ID
     * @return Key
     */
    public static String buildUserPreferencesKey(Long userId) {
        return USER_PREFERENCES_PREFIX + userId;
    }

    /**
     * 构建验证码 Key
     * @param key 验证码标识
     * @return Key
     */
    public static String buildCaptchaKey(String key) {
        return CAPTCHA_PREFIX + key;
    }

    /**
     * 构建接口限流 Key
     * @param api 接口标识
     * @return Key
     */
    public static String buildRateLimitKey(String api) {
        return RATE_LIMIT_PREFIX + api;
    }

    /**
     * 构建用户操作限流 Key
     * @param userId 用户ID
     * @param operation 操作类型
     * @return Key
     */
    public static String buildUserRateLimitKey(Long userId, String operation) {
        return USER_RATE_LIMIT_PREFIX + userId + ":" + operation;
    }

    /**
     * 构建通知 Key
     * @param userId 用户ID
     * @param notificationId 通知ID
     * @return Key
     */
    public static String buildNotificationKey(Long userId, String notificationId) {
        return NOTIFICATION_PREFIX + userId + ":" + notificationId;
    }
}