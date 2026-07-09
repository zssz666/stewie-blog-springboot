package com.stewie.blog.dto.vo;

import lombok.Data;

/**
 * 标签视图对象
 */
@Data
public class TagVO {

    private Long id;
    private String name;
    private String slug;
    /** 该标签下文章数（可选） */
    private Long count;
}
