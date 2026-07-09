package com.stewie.blog.dto.vo;

import lombok.Data;

/**
 * 分类视图对象
 */
@Data
public class CategoryVO {

    private Long id;
    private String name;
    private String slug;
    private Integer sort;
    /** 该分类下文章数（可选） */
    private Long count;
}
