package com.stewie.blog.controller.admin;

import com.stewie.blog.common.PageResult;
import com.stewie.blog.common.Result;
import com.stewie.blog.dto.request.ReviewRequest;
import com.stewie.blog.dto.vo.CommentVO;
import com.stewie.blog.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理 API - 评论审核 / 删除（需 JWT）
 * <p>路径位于 /api/admin/** 下，由 SecurityConfig 的 anyRequest().authenticated() 保护。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 评论列表（可按文章 / 状态过滤）
     */
    @GetMapping("/comments")
    public Result<PageResult<CommentVO>> list(
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(commentService.listForAdmin(postId, status, page, size));
    }

    /**
     * 审核评论（status: 1=通过 2=垃圾）
     */
    @PutMapping("/comments/{id}")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest body) {
        commentService.reviewComment(id, body.getStatus());
        return Result.success();
    }

    /**
     * 删除评论（逻辑删除）
     */
    @DeleteMapping("/comments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }
}
