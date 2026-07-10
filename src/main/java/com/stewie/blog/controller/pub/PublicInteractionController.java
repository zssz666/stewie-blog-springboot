package com.stewie.blog.controller.pub;

import com.stewie.blog.common.Result;
import com.stewie.blog.dto.request.CommentRequest;
import com.stewie.blog.dto.vo.CommentVO;
import com.stewie.blog.dto.vo.LikeVO;
import com.stewie.blog.service.CommentService;
import com.stewie.blog.service.LikeService;
import com.stewie.blog.util.ClientFingerprintUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 公开 API - 互动模块（点赞 / 评论）
 * <p>路径前缀 /api，且位于 /api/posts/** 下，由 SecurityConfig 放行为公开。</p>
 */
@RestController
@RequestMapping("/api")
public class PublicInteractionController {

    private final LikeService likeService;
    private final CommentService commentService;

    public PublicInteractionController(LikeService likeService, CommentService commentService) {
        this.likeService = likeService;
        this.commentService = commentService;
    }

    /**
     * 当前访客对某文章的点赞状态（指纹去重）
     */
    @GetMapping("/posts/{id}/like")
    public Result<LikeVO> likeStatus(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(likeService.getStatus(id, ClientFingerprintUtil.getFingerprint(request)));
    }

    /**
     * 点赞（幂等：同一指纹重复点赞不会重复 +1）
     */
    @PostMapping("/posts/{id}/like")
    public Result<LikeVO> like(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(likeService.like(id, ClientFingerprintUtil.getFingerprint(request)));
    }

    /**
     * 某文章的已通过评论（树形）
     */
    @GetMapping("/posts/{id}/comments")
    public Result<List<CommentVO>> comments(@PathVariable Long id) {
        return Result.success(commentService.listApprovedTree(id));
    }

    /**
     * 提交评论（默认待审核，审核通过后展示）
     */
    @PostMapping("/posts/{id}/comments")
    public Result<Map<String, Long>> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest body,
            HttpServletRequest request) {
        Long cid = commentService.createComment(id, body, ClientFingerprintUtil.getClientIp(request));
        return Result.success(Map.of("id", cid));
    }
}
