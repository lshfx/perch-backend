package com.perch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.perch.entity.ChatMessages;
import org.springframework.stereotype.Repository;

/**
 * (ChatMessages)表数据库访问层
 *
 * @author lsh
 * @since 2025-11-20 13:51:18
 */
@Repository
public interface ChatMessagesMapper extends BaseMapper<ChatMessages> {

}

