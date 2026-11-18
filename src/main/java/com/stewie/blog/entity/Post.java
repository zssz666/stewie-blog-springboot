package com.stewie.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 文章实体（对应 t_post）
 */
@Data
@TableName("t_post")
public class Post {

    public static final int TYPE_QUICK_NOTE = 1;
    public static final int TYPE_TECHNICAL_ARTICLE = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String slug;
    private String title;
    private String excerpt;
    private String content;
    private String coverColor;
    private String cover;
    private Long categoryId;
    private Integer postType;
    private LocalDate publishDate;
    private Integer readingTime;
    private Long views;
    private Long likes;
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;

    public static boolean isValidPostType(int postType) {
        return postType == TYPE_QUICK_NOTE || postType == TYPE_TECHNICAL_ARTICLE;
    }
}
