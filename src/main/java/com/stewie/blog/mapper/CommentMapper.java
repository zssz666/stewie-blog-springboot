package com.stewie.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stewie.blog.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT COUNT(*) FROM t_comment WHERE deleted = 0 AND status = 1")
    long countApproved();

    @Select("SELECT COUNT(*) FROM t_comment WHERE deleted = 0 AND status = 0")
    long countPending();
}
