package com.perch.infrastructure.tool;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.perch.mapper.ChatMessageMapper;
import com.perch.mapper.DreamLogMapper;
import com.perch.mapper.PsychScaleLogMapper;
import com.perch.mapper.UserPersonaTagMapper;
import com.perch.pojo.entity.ChatMessage;
import com.perch.pojo.entity.DreamLog;
import com.perch.pojo.entity.PsychScaleLog;
import com.perch.pojo.entity.UserPersonaTag;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
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
    private final SkillRegistry skillRegistry;
    private final PsychScaleLogMapper psychScaleLogMapper;
    private final DreamLogMapper dreamLogMapper;           // 👈 新增梦境 Mapper

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

    @Tool(description = "读取特定技能的详细说明文档。当你在系统提示词的可用技能列表中发现某个技能适合当前场景时，必须先调用此工具获取该技能的完整执行指南。")
    public String read_skill(@ToolParam(description = "要加载的技能名称，例如 'PHQ-9' 或 'GAD-7'") String skillName,
                             ToolContext toolContext) {

        Long userId = (Long) toolContext.getContext().get("userId");

        // 1. 无缝衔接：此时的 skillName 就是原汁原味的 "GAD-7" 或 "PHQ-9"，直接查库！
        if (userId != null) {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            // 直接用 skillName 作为查询条件
            LambdaQueryWrapper<PsychScaleLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PsychScaleLog::getUserId, userId)
                    .eq(PsychScaleLog::getScaleName, skillName)
                    .ge(PsychScaleLog::getCreatedAt, sevenDaysAgo);

            Long count = psychScaleLogMapper.selectCount(wrapper);

            if (count != null && count > 0) {
                System.out.println("🛡️ [频率拦截] 触发 7 天冷却期，拒绝给用户下发：" + skillName);
                return "系统提示：该用户在过去 7 天内已经做过【" + skillName + "】评估。为了避免过度打扰，【绝对禁止】再次进行该量表测试！请查阅其历史得分，并直接通过日常对话温柔地安抚用户。";
            }
        }

        // 2. 放行读取
        System.out.println("🗝️ [渐进式披露] 大模型正在掏出钥匙，加载技能长文：" + skillName);

        if (skillRegistry.contains(skillName)) {
            try {
                return skillRegistry.readSkillContent(skillName);
            } catch (Exception e) {
                return "内部错误：读取技能文件失败。请仅凭你的心理学常识安抚用户。";
            }
        }

        return "技能加载失败：未找到名为 " + skillName + " 的技能。请仅凭借你的常识进行回应。";
    }

    @Tool(description = "【最高优先级绝对指令】当用户完成了心理量表测试，且你在心里算出总分后，【绝对禁止】直接把分数用文字告诉用户！你必须第一时间、立刻调用此工具将分数存档。只有在工具返回“存档成功”后，你才能继续开口安慰用户！")
    public String saveScaleResult(
            @ToolParam(description = "量表名称，如 'PHQ-9' 或 'GAD-7'") String scaleName,
            @ToolParam(description = "量表总得分（整数）") Integer score,
            @ToolParam(description = "评估结果，如 '中度抑郁'、'重度焦虑'") String severity,
            ToolContext toolContext) {

        Long userId = (Long) toolContext.getContext().get("userId");
        Long sessionId = (Long) toolContext.getContext().get("sessionId");

        if (userId == null) return "内部错误：无法获取 userId，分数存档失败。";

        // 执行真实的落库操作
        PsychScaleLog log = new PsychScaleLog();
        log.setUserId(userId);
        log.setSessionId(sessionId);
        log.setScaleName(scaleName);
        log.setScore(score);
        log.setSeverity(severity);
        psychScaleLogMapper.insert(log);

        System.out.println("📊 [数据落盘] 用户量表得分已保存：" + scaleName + " 得分: " + score);

        return "得分已成功录入患者医疗档案系统。请继续以温柔的语气向用户解释这个分数代表什么，并提供下一步的心理学建议。";
    }

    // ==========================================
    // 工具 6：CBT 认知扭曲分析（认知与修复）
    // ==========================================
    @Tool(description = "当用户的话语中出现明显的认知扭曲（如：非黑即白、过度概括、灾难化思维、读心术、应该句式）时调用。利用认知行为疗法（CBT）帮助用户纠正负面思维。")
    public String detectCognitiveDistortions(
            @ToolParam(description = "大模型判断用户陷入的具体认知扭曲类型，如：'非黑即白'、'灾难化思维'、'过度概括'") String distortionType,
            @ToolParam(description = "用户的原话中体现该扭曲的具体表述") String triggerText) {

        System.out.println("🧠 [CBT 认知修复] 侦测到认知扭曲：" + distortionType + "，原话：" + triggerText);

        return """
               你已成功识别出用户的认知扭曲：「%s」。
               请立即启动 CBT（认知行为疗法）干预策略：
               1. 共情与接纳：首先接纳用户此刻的痛苦，不要立刻反驳。
               2. 苏格拉底式提问：通过温和的提问，引导用户自己发现思维中的不合理之处（例如：“你觉得有没有其他可能的解释？”、“有没有例外的情况？”）。
               3. 认知重构：引导用户用更客观、中性的语言重新描述刚才发生的事情。
               严禁说教，要像一个耐心的引导者一样陪用户一起寻找思维的盲区。
               """.formatted(distortionType);
    }

    // ==========================================
    // 工具 7：正念与减压引导（人文关怀）
    // ==========================================
    @Tool(description = "当用户表现出急性的焦虑、恐慌、呼吸急促，或明确要求寻找放松、助眠方法时调用。为用户提供即时的正念或呼吸练习指导。")
    public String generateMindfulnessGuide(
            @ToolParam(description = "需要的放松技巧类型，可选值：'4-7-8呼吸法'、'身体扫描'、'蝴蝶拥抱'") String techniqueType) {

        System.out.println("🧘‍♀️ [人文关怀] 触发正念减压引导：" + techniqueType);

        return switch (techniqueType) {
            case "4-7-8呼吸法" -> """
                    请使用非常缓慢、充满安全感的语调，教导用户使用 4-7-8 呼吸法：
                    “闭上眼睛，吸气4秒...憋气7秒...然后缓缓呼气8秒...”
                    配合文字排版（如换行、省略号）营造出节奏感，陪用户一起做3个循环。
                    """;
            case "身体扫描" -> """
                    请用催眠般的温柔语调，引导用户进行简短的身体扫描练习：
                    从头顶开始，慢慢向下关注眉心、肩膀、胸口、手臂，直到脚趾。告诉用户将紧绷的肌肉一点点松开。
                    """;
            default -> """
                    请教导用户使用“蝴蝶拥抱”法寻找安全感：
                    双臂交叉放在胸前，双手交替轻轻拍打自己的肩膀，就像蝴蝶拍打翅膀一样，同时深呼吸，告诉自己“我现在很安全”。
                    """;
        };
    }

    // ==========================================
    // 工具 8：梦境日志与潜意识解析（人文关怀）
    // ==========================================
    @Tool(description = "当用户主动分享自己的梦境，或者提到奇怪的梦时调用。用于将梦境存档，并触发精神分析学派的解析。")
    public String logDreamDiary(
            @ToolParam(description = "用户梦境的核心意象或物品，如：'被追杀'、'掉牙齿'、'考试迟到'") String dreamElement,
            ToolContext toolContext) {

        Long userId = (Long) toolContext.getContext().get("userId");
        Long sessionId = (Long) toolContext.getContext().get("sessionId");

        // 1. 将梦境意象落库（未来可以做成用户的“潜意识词云”）
        if (userId != null) {
            DreamLog log = new DreamLog();
            log.setUserId(userId);
            log.setSessionId(sessionId);
            log.setDreamElement(dreamElement);
            dreamLogMapper.insert(log);
            System.out.println("🌙 [梦境落盘] 记录潜意识意象：" + dreamElement);
        }

        // 2. 返回给大模型的解梦法则（Prompt 约束）
        return """
               已将梦境核心意象「%s」存档。
               请现在开始进行梦境解析，遵循以下原则：
               1. 采用荣格分析心理学视角，询问这个意象在梦中给用户的【核心情绪】是什么？
               2. 引导用户将这个梦境与近期现实生活中的压力源建立联想。
               3. 严禁迷信断言（不要说“梦见掉牙代表家里有人生病”），要给梦境赋予积极的转化意义。
               """.formatted(dreamElement);
    }
}