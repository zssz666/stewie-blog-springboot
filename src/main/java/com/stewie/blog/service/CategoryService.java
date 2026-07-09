package com.stewie.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stewie.blog.dto.vo.CategoryVO;
import com.stewie.blog.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /** 分类列表（按 sort 排序） */
    List<CategoryVO> listCategories();
}
