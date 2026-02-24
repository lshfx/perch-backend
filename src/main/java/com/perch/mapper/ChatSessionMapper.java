package com.perch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.perch.pojo.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * (ChatSession)表数据库访问层
 *
 * @author lsh
 * @since 2025-11-20 13:51:18
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

}

