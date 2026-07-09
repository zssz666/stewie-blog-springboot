package com.stewie.blog.controller.pub;

import com.stewie.blog.common.Result;
import com.stewie.blog.dto.vo.AuthorVO;
import com.stewie.blog.dto.vo.CategoryVO;
import com.stewie.blog.dto.vo.TagVO;
import com.stewie.blog.service.AuthorService;
import com.stewie.blog.service.CategoryService;
import com.stewie.blog.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开 API - 分类 / 标签 / 作者
 */
@RestController
@RequestMapping("/api")
public class PublicMiscController {

    private final CategoryService categoryService;
    private final TagService tagService;
    private final AuthorService authorService;

    public PublicMiscController(CategoryService categoryService, TagService tagService, AuthorService authorService) {
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.authorService = authorService;
    }

    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.success(categoryService.listCategories());
    }

    @GetMapping("/tags")
    public Result<List<TagVO>> tags() {
        return Result.success(tagService.listTags());
    }

    @GetMapping("/author")
    public Result<AuthorVO> author() {
        return Result.success(authorService.getAuthor());
    }
}
