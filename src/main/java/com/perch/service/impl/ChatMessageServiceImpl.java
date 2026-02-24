package com.perch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.perch.mapper.ChatMessageMapper;
import com.perch.pojo.entity.ChatMessage;
import com.perch.service.ChatMessageService;
import org.springframework.stereotype.Service;

/**
 * Chat message service implementation.
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {
}
