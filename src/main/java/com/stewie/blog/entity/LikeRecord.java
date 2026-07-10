package com.stewie.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞记录实体（对应 t_like_record，指纹防重复点赞）
 */
@Data
@TableName("t_like_record")
public class LikeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    /** 用户指纹（IP+UA hash） */
    private String fingerprint;

    private LocalDateTime createdAt;
}
