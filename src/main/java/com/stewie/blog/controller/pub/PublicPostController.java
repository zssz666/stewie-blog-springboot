package com.stewie.blog.controller.pub;

import com.stewie.blog.common.PageResult;
import com.stewie.blog.common.Result;
import com.stewie.blog.common.ResultCode;
import com.stewie.blog.dto.vo.PostVO;
import com.stewie.blog.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开 API - 文章
 */
@RestController
@RequestMapping("/api")
public class PublicPostController {

    private final PostService postService;

    public PublicPostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 文章列表（已发布，按发布日期倒序，分页 + 可选 分类/标签 过滤）
     *
     * @param page     页码，从 1 开始，默认 1
     * @param size     每页条数，默认 10（后端限制 1~100）
     * @param category 分类名（可选）
     * @param tag      标签名（可选）
     */
    @GetMapping("/posts")
    public Result<PageResult<PostVO>> listPosts(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {
        return Result.success(postService.pagePublishedPosts(page, size, category, tag));
    }

    /**
     * 热门文章 Top 5（放在 {slug} 前，精确路径优先匹配）
     */
    @GetMapping("/posts/popular")
    public Result<List<PostVO>> popularPosts() {
        return Result.success(postService.getPopularPosts(5));
    }

    /**
     * 全文搜索（ngram 中文分词），分页返回已发布文章
     */
    @GetMapping("/search")
    public Result<PageResult<PostVO>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(postService.searchPosts(q, page, size));
    }

    /**
     * 文章详情（按 slug）
     */
    @GetMapping("/posts/{slug}")
    public Result<PostVO> getPost(@PathVariable String slug) {
        PostVO vo = postService.getPostBySlug(slug);
        return vo != null ? Result.success(vo) : Result.error(ResultCode.NOT_FOUND);
    }

    /**
     * 浏览量埋点（匿名可调用）：文章被打开时 +1，不返回内容
     */
    @PostMapping("/posts/{id}/view")
    public Result<Void> incrementViews(@PathVariable Long id) {
        postService.incrementViews(id);
        return Result.success();
    }
}
