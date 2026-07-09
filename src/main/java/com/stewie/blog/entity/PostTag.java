package com.stewie.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章-标签关联（对应 t_post_tag）
 */
@Data
@TableName("t_post_tag")
public class PostTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long tagId;

    private LocalDateTime createdAt;
}
