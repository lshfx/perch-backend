package com.perch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.perch.mapper.ChatSessionMapper;
import com.perch.pojo.entity.ChatSession;
import com.perch.service.ChatSessionService;
import org.springframework.stereotype.Service;

/**
 * Chat session service implementation.
 */
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {
}
