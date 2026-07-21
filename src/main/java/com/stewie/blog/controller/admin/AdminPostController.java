package com.stewie.blog.controller.admin;

import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.PageResult;
import com.stewie.blog.common.Result;
import com.stewie.blog.common.ResultCode;
import com.stewie.blog.dto.request.ExcerptGenRequest;
import com.stewie.blog.dto.request.PostRequest;
import com.stewie.blog.dto.vo.PostVO;
import com.stewie.blog.service.AiService;
import com.stewie.blog.service.PostService;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理 API - 文章 CRUD（需 JWT）
 * <p>路径位于 /api/admin/posts 下，由 SecurityConfig 保护。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminPostController {

    private final PostService postService;
    private final AiService aiService;

    public AdminPostController(PostService postService, AiService aiService) {
        this.postService = postService;
        this.aiService = aiService;
    }

    /**
     * 文章列表（含草稿，可按状态过滤）
     */
    @GetMapping("/posts")
    public Result<PageResult<PostVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(postService.adminPagePosts(page, size, status));
    }

    /**
     * 单篇文章（编辑回显）
     */
    @GetMapping("/posts/{id}")
    public Result<PostVO> getOne(@PathVariable Long id) {
        PostVO vo = postService.getAdminPost(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        return Result.success(vo);
    }

    /**
     * 新建文章
     */
    @PostMapping("/posts")
    public Result<PostVO> create(@Valid @RequestBody PostRequest body) {
        Long id = postService.createPost(body);
        return Result.success(postService.getAdminPost(id));
    }

    /**
     * 更新文章
     */
    @PutMapping("/posts/{id}")
    public Result<PostVO> update(@PathVariable Long id, @Valid @RequestBody PostRequest body) {
        postService.updatePost(id, body);
        return Result.success(postService.getAdminPost(id));
    }

    /**
     * 删除文章（逻辑删除）
     */
    @DeleteMapping("/posts/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success();
    }

    /**
     * AI 生成摘要：根据标题与正文（HTML）生成一句话摘要
     * <p>需登录；正文 HTML 由服务端转换为纯文本后提交给大模型。</p>
     */
    @PostMapping("/posts/generate-excerpt")
    public Result<Map<String, String>> generateExcerpt(@RequestBody ExcerptGenRequest body) {
        String excerpt = aiService.generateExcerpt(body.getTitle(), body.getContent());
        return Result.success(Map.of("excerpt", excerpt));
    }
}
