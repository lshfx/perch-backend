package com.perch.infrastructure.tool;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.perch.mapper.ChatMessageMapper;
import com.perch.pojo.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PsychologicalTools {

    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;

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
        returnDirect = true // 关键：开启此选项后，工具返回值将绕过大模型，直接作为最终结果发送给前端！
    )
    public String triggerEmergencyProtocol(
            @ToolParam(description = "触发危机的原话片段") String triggerText,
            ToolContext toolContext) {

        RunnableConfig config = (RunnableConfig) toolContext.getContext().get("config");
        String userId = (String) config.metadata("user_id").orElse("anonymous");

        // 1. 触发你后端的 JWT 黑名单/封禁逻辑，或者给管理员发 WebSocket 紧急警报
        // alertService.triggerHighRiskAlert(userId, triggerText);
        
        System.out.println("🚨 [警告] 触发危机干预协议！用户：" + userId + "，危险文本：" + triggerText);

        // 2. 由于 returnDirect = true，这段字符串会直接通过你的 SSE 接口流向客户端
        return """
               系统检测到您当前可能正在经历极度的痛苦。您的生命对我们非常重要。
               大模型对话已暂时挂起。
               请立即拨打以下 24 小时免费心理援助热线：
               【希望 24 小时热线】：400-161-9995
               【抑郁症援助专线】：010-82951332
               我们一直都在，请给生命一次求救的机会。
               """;
    }
}