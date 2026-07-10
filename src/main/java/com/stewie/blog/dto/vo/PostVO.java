package com.stewie.blog.dto.vo;

import lombok.Data;

import java.util.List;

/**
 * 文章视图对象（对齐前端 Post 接口）
 * 前端字段：id/slug/title/excerpt/content/cover/coverColor/category/tag/tags/date/readingTime/views
 */
@Data
public class PostVO {

    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String content;
    private String cover;
    private String coverColor;

    /** 分类名（由 category_id 关联 t_category.name） */
    private String category;

    /** 标签名（前端单标签，取首个，向后兼容） */
    private String tag;

    /** 标签名列表（多标签） */
    private List<String> tags;

    /** 发布日期 yyyy-MM-dd */
    private String date;

    private Integer readingTime;
    private Long views;
    private Long likes;

    /** 状态：0=草稿 1=已发布 */
    private Integer status;
}
