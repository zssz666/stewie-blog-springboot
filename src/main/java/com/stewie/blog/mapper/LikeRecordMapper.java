package com.stewie.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stewie.blog.entity.LikeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞记录 Mapper
 */
@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {
}
