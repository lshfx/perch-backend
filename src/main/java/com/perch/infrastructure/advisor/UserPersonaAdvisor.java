package com.perch.infrastructure.advisor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.perch.mapper.UserPersonaTagMapper;
import com.perch.pojo.entity.UserPersonaTag;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UserPersonaAdvisor implements CallAdvisor, StreamAdvisor {

    private final UserPersonaTagMapper personaTagMapper;

    public UserPersonaAdvisor(UserPersonaTagMapper personaTagMapper) {
        this.personaTagMapper = personaTagMapper;
    }

    @Override
    public String getName() {
        return "UserPersonaAdvisor";
    }

    @Override
    public int getOrder() {
        // 优先级设置高一点，确保能在其他业务处理前把系统提示词组装好
        return -10;
    }

    /**
     * 核心逻辑：从数据库查标签，并注入到 System Text 中
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 拦截同步请求
        return chain.nextCall(this.enhanceRequestWithPersona(request));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // 拦截流式请求
        return chain.nextStream(this.enhanceRequestWithPersona(request));
    }

    /**
     * 核心逻辑：从数据库查标签，并注入到 System Text 中
     */
    private ChatClientRequest enhanceRequestWithPersona(ChatClientRequest request) {
        // 1. 获取上下文中传递的 userId（新版 API 通常通过 advisorParams 获取）
        Map<String, Object> context = request.context();

        if (context == null || !context.containsKey("userId")) {
            return request; // 如果没有传 userId，直接放行
        }

        Long userId = (Long) context.get("userId");

        // 2. 从 PostgreSQL 中捞取该用户所有的历史画像标签
        List<UserPersonaTag> tags = personaTagMapper.selectList(Wrappers.<UserPersonaTag>lambdaQuery()
                .eq(UserPersonaTag::getUserId, userId)
                .orderByDesc(UserPersonaTag::getCreatedAt)
        );

        if (tags.isEmpty()) {
            return request;
        }

        // 3. 组装成一段给大模型看的“系统设定”文本
        String tagsStr = tags.stream()
                .map(t -> String.format("【%s】(分析依据: %s)", t.getTagName(), t.getReasoning()))
                .collect(Collectors.joining("\n"));

        String personaContext = "\n\n[后台绝密档案：当前用户的长期心理画像]\n" +
                "你之前已经通过观察，为该用户总结了以下性格标签。请在本次回复中深刻结合这些特质进行共情，但绝不要直接向用户暴露这些专业术语：\n" +
                tagsStr;

        String originalSystemText = request.prompt().getSystemMessage().getText();

        // 利用 Prompt 原生的 augmentSystemMessage 直接生成包含新系统提示词的 Prompt
        Prompt updatedPrompt = request.prompt().augmentSystemMessage(originalSystemText + personaContext);

        // 利用 ChatClientRequest 的 mutate() 方法，保留原有的 context，只替换 prompt
        return request.mutate()
                .prompt(updatedPrompt)
                .build();
    }
}