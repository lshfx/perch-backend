package com.perch.config;

import com.perch.infrastructure.advisor.PostgresStreamArchiveAdvisor;
import com.perch.infrastructure.memory.RedisChatMemory;
import com.perch.service.ChatMessageService;
import com.perch.service.EmotionAnalysisService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class AiConfig {

    /**
     * 1. 声明查资料工具为 Spring Bean。
     * 神奇机制：加上 @Description 注解，Spring AI 会自动把它识别为一个大模型可以调用的 Function Tool！
     */
    @Bean
    @Description("当用户表达负面情绪、心理困扰，或者你需要专业的心理学知识（如情绪急救、CBT疗法）来提供干预建议时，请主动调用此工具搜索知识库。如果只是日常寒暄，无需调用。")
    public Function<DocumentSearchTool.Request, DocumentSearchTool.Response> searchPsychologyKnowledge(VectorStore vectorStore) {
        return request -> {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(request.query()).topK(3).similarityThreshold(0.60).build()
            );
            if (docs.isEmpty()) {
                return new DocumentSearchTool.Response("未找到具体的心理学策略，请仅凭借你的共情能力给予安抚。");
            }
            return new DocumentSearchTool.Response(docs.stream().map(org.springframework.ai.document.Document::getText).collect(Collectors.joining("\n---\n")));
        };
    }

    @Bean
    public OllamaChatModel ollamaChatModel() {
        // 1. 初始化底层 API 客户端 (指向本地默认端口)
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl("http://localhost:11434")
                .build();

        // 2. 构建并返回 OllamaChatModel，带上默认的低温度和小模型配置
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model("qwen3:0.6b") // 你测试通过的模型
                        .temperature(0.1)    // 低温度，适合提取数据
                        .format("json")
                        .build())
                .build();
    }

    /**
     * 这里不再需要手动定义 OpenAiChatModel 或 DeepSeekChatModel。
     * Spring AI 的自动配置（Auto-configuration）会根据 application.yml 中的配置
     * 自动创建一个 ChatModel Bean，并注入到这里的参数中。
     */
    @Bean
    public ChatClient chatClient(
            @Qualifier("deepSeekChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            PostgresStreamArchiveAdvisor postgresAdvisor) {

        return ChatClient.builder(chatModel)
                // 全局树洞人设
                .defaultSystem("""
                            你是一个温暖、充满共情能力的“心理树洞”和知心朋友。你的任务是倾听用户的烦恼，并给予温柔的抚慰和专业的心理疏导。
                            当遇到棘手的心理问题时，你可以使用 searchPsychologyKnowledge 工具来查阅专业书籍。
                            
                            回答的绝对铁律：
                            1. 语气极度温柔，像一个真实的朋友在耳边轻语。
                            2. 严禁出现“根据搜索结果”、“资料显示”、“作为AI”等暴露查阅资料或机器身份的词汇。请将查到的知识内化为你自己的想法说出来。
                            3. 先肯定和接纳用户的情绪，再巧妙运用心理学技巧给出轻量级建议。
                            4. 保持第一人称口吻进行对话。
                        """)
                // 全局绑定刚刚写的 @Bean 工具，注意名字是方法名
                .defaultToolNames("searchPsychologyKnowledge")
                // 全局绑定记忆顾问（这里不写死 ID）
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 全局绑定 Postgres 持久化归档顾问
                .defaultAdvisors(postgresAdvisor)
                .build();
    }

    // 保留你的自定义 Advisor 逻辑
    @Bean
    public PostgresStreamArchiveAdvisor postgresAdvisor(
            ChatMessageService chatMessageService,
            @Qualifier("archiveTaskExecutor") Executor executor,
            EmotionAnalysisService emotionAnalysisService) {
        return new PostgresStreamArchiveAdvisor(chatMessageService, executor, emotionAnalysisService);
    }

    // 保留 Redis 聊天记忆配置
    @Bean
    public ChatMemory chatMemory(RedisTemplate<String, Object> redisTemplate) {
        // 这里的 20 是指保留最近 20 条消息上下文，可根据 DeepSeek 的 context window 调整
        return new RedisChatMemory(redisTemplate, 20);
    }

    public static class DocumentSearchTool {
        public record Request(String query) {
        }

        public record Response(String content) {
        }
    }
}
