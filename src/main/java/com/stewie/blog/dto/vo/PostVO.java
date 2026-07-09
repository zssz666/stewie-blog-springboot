package com.stewie.blog.dto.vo;

import lombok.Data;

/**
 * 文章视图对象（对齐前端 Post 接口）
 * 前端字段：id/slug/title/excerpt/content/coverColor/category/tag/date/readingTime/views
 */
@Data
public class PostVO {

    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String content;
    private String coverColor;

    /** 分类名（由 category_id 关联 t_category.name） */
    private String category;

    /** 标签名（前端单标签，取首个） */
    private String tag;

    /** 发布日期 yyyy-MM-dd */
    private String date;

    private Integer readingTime;
    private Long views;
    private Long likes;
}
