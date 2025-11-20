package com.perch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.perch.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis Plus 自动提供 CRUD 方法
}