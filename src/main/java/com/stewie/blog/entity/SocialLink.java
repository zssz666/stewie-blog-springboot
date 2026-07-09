package com.stewie.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社交链接（对应 t_social_link）
 */
@Data
@TableName("t_social_link")
public class SocialLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long authorId;
    private String label;
    private String href;
    private String icon;
    private Integer sort;

    private LocalDateTime createdAt;
}
