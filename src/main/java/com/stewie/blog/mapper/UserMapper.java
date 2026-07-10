package com.stewie.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stewie.blog.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员账户 Mapper（对应 t_user）
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
