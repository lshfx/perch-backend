package com.perch.infrastructure.tool;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.perch.mapper.ChatMessageMapper;
import com.perch.mapper.UserPersonaTagMapper;
import com.perch.pojo.entity.ChatMessage;
import com.perch.pojo.entity.UserPersonaTag;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PsychologicalTools {

    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;
    private final UserPersonaTagMapper personaTagMapper;
    private final VectorStore vectorStore;

    /**
     * 工具：调取情绪历史（Memory & Evolution）
     */
    @Tool(description = "当用户表达长期困扰，或者你需要了解用户近期的情绪起伏、前因后果时调用此工具。它会返回该用户最近的真实情绪档案和原话。")
    public String getUserEmotionalHistory(ToolContext toolContext) {

        // 1. 获取当前会话 ID
        Long sessionId = (Long) toolContext.getContext().get("sessionId");

        if (sessionId == null) {
            return "无法获取当前用户的会话 ID，无法查询情绪历史。请仅基于当前对话内容进行回应。";
        }

        System.out.println("🔍 [工具调用] DeepSeek 正在查询 Session " + sessionId + " 的历史情绪档案...");

        // 2. 真实数据库查询：查出该会话下，角色是 user 且含有 emotion_tag 的最新 10 条记录
        List<ChatMessage> recentMessages = chatMessageMapper.selectList(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getRole, "user")
                .isNotNull(ChatMessage::getEmotionTag) // 只要打过标签的
                .orderByDesc(ChatMessage::getCreatedAt) // 按时间倒序，取最新的
                .last("LIMIT 10")
        );

        if (recentMessages.isEmpty()) {
            return "该用户近期暂无情绪档案记录。";
        }

        // 3. 将结果翻转为正序（最旧的在前面，方便大模型按照时间线阅读）
        Collections.reverse(recentMessages);

        // 4. 将 JSON 格式的标签解析并组装成大模型易读的自然语言文本
        StringBuilder sb = new StringBuilder("[近期情绪档案记录]:\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (ChatMessage msg : recentMessages) {
            try {
                // 解析 Ollama 打的标签：{"type":"焦虑","score":4}
                JsonNode tagNode = objectMapper.readTree(msg.getEmotionTag());
                String emotionType = tagNode.has("type") ? tagNode.get("type").asText() : "未知";
                int score = tagNode.has("score") ? tagNode.get("score").asInt() : 0;

                String timeStr = msg.getCreatedAt().format(formatter);

                // 拼装：- [02-27 12:28] 核心情绪:【焦虑】(强度: 4)，触发原话: "我该怎么融入他们..."
                sb.append(String.format("- [%s] 核心情绪:【%s】(强度: %d)，触发原话: \"%s\"\n",
                        timeStr, emotionType, score, msg.getContent()));
            } catch (Exception e) {
                System.err.println("解析情绪标签失败：" + e.getMessage());
            }
        }

        String finalResult = sb.toString();
        System.out.println("✅ [工具返回数据]：\n" + finalResult);

        return finalResult;
    }

    /**
     * 工具 2：危机干预红色按钮（阻断与直接返回）
     * 场景：检测到极端的负面意图，立即停止大模型自由发挥，下发专业热线。
     */
    @Tool(
            description = "当用户表达出明显的自残、自杀意图，或处于极度危险的心理危机时，必须立即调用此工具。",
            returnDirect = true
    )
    public String triggerEmergencyProtocol(
            @ToolParam(description = "触发危机的原话片段") String triggerText,
            ToolContext toolContext) {

        // ✅ 直接从上下文中获取我们之前塞进去的 userId
        Long userId = (Long) toolContext.getContext().get("userId");
        String userIdentifier = userId != null ? String.valueOf(userId) : "anonymous";

        System.out.println("🚨 [警告] 触发危机干预协议！用户：" + userIdentifier + "，危险文本：" + triggerText);

        return """
               系统检测到您当前可能正在经历极度的痛苦。您的生命对我们非常重要。
               大模型对话已暂时挂起。
               请立即拨打以下 24 小时免费心理援助热线：
               【希望 24 小时热线】：400-161-9995
               【抑郁症援助专线】：010-82951332
               我们一直都在，请给生命一次求救的机会。
               """;
    }

    /*
     * 工具 3：构建独立的用户画像
     * 场景：当大模型在多轮对话中，敏锐地“嗅”到用户性格底色时，它会主动触发这个工具
     *
     */
    @Tool(description = "当在对话中深刻察觉到用户的长期性格底色、依恋模式或核心心理防御机制时，强制调用此工具为用户打上心理学标签（如：回避型依恋、讨好型人格、非黑即白思维等）。")
    public String updatePersonaTags(
            @ToolParam(description = "要添加的心理学标签，尽量使用专业且中性的心理学词汇。") String tagName,
            @ToolParam(description = "结合用户的原话，给出贴上该标签的深度分析和理由。") String reasoning,
            ToolContext toolContext) {

        // 1. 从工具上下文中获取当前的 sessionId和userId
        Long sessionId = (Long) toolContext.getContext().get("sessionId");
        Long userId = (Long) toolContext.getContext().get("userId");

        if (userId == null) {
            return "内部错误：无法获取userId，画像保存失败";
        }

        if (sessionId == null) {
            return "内部错误：无法获取 sessionId，画像保存失败。";
        }

        // 2. 构造实体并保存到 PostgreSQL
        UserPersonaTag tag = new UserPersonaTag();
        tag.setUserId(userId);
        tag.setSessionId(sessionId);
        tag.setTagName(tagName);
        tag.setReasoning(reasoning);
        personaTagMapper.insert(tag);

        System.out.println("🧠 [画像进化] 大模型主动为 Session " + sessionId + " 贴上标签：【" + tagName + "】");

        // 3. 返回给大模型的执行结果
        return "用户画像标签【" + tagName + "】已成功存入长期记忆数据库。在接下来的回复中，请隐式地结合此画像特征，提供更具针对性的抱持与关怀，但不要生硬地说出该标签名称。";
    }

    /*
     * 工具 4：Rag知识库检索
     * 场景：当用户表达负面情绪、心理困扰，或者你需要专业的心理学知识（如情绪急救、CBT疗法）来提供干预建议时，请主动调用此工具搜索知识库
     *
     */
    @Tool(description = "当用户表达负面情绪、心理困扰，或者你需要专业的心理学知识（如情绪急救、CBT疗法）来提供干预建议时，请主动调用此工具搜索知识库。如果只是日常寒暄，无需调用。")
    public String searchPsychologyKnowledge(
            @ToolParam(description = "要搜索的心理学关键词或相关问题描述") String query) { // 👈 直接接收 String，不再需要 Request 对象

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).similarityThreshold(0.60).build()
        );

        if (docs.isEmpty()) {
            return "未找到具体的心理学策略，请仅凭借你的共情能力给予安抚。";
        }

        // 直接返回拼接好的字符串，不再需要 Response 对象
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }
}