package com.stewie.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.PageResult;
import com.stewie.blog.common.ResultCode;
import com.stewie.blog.dto.request.CommentRequest;
import com.stewie.blog.dto.vo.CommentVO;
import com.stewie.blog.entity.Comment;
import com.stewie.blog.entity.Post;
import com.stewie.blog.mapper.CommentMapper;
import com.stewie.blog.mapper.PostMapper;
import com.stewie.blog.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论服务实现
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public List<CommentVO> listApprovedTree(Long postId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getCreatedAt));
        return buildTree(comments, false);
    }

    @Override
    public Long createComment(Long postId, CommentRequest req, String ip) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        // 回复场景：校验父评论归属同一文章
        if (req.getParentId() != null) {
            Comment parent = commentMapper.selectById(req.getParentId());
            if (parent == null || !postId.equals(parent.getPostId())) {
                throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
            }
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setParentId(req.getParentId());
        comment.setNickname(req.getNickname().trim());
        comment.setEmail(StringUtils.hasText(req.getEmail()) ? req.getEmail().trim() : null);
        comment.setContent(req.getContent().trim());
        comment.setStatus(0); // 待审核
        comment.setIp(ip);
        commentMapper.insert(comment);
        return comment.getId();
    }

    @Override
    public PageResult<CommentVO> listForAdmin(Long postId, Integer status, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 20 : Math.min(size, 200);

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(postId != null, Comment::getPostId, postId)
                .eq(status != null, Comment::getStatus, status)
                .orderByDesc(Comment::getCreatedAt);

        Page<Comment> pageParam = new Page<>(safePage, safeSize);
        Page<Comment> result = commentMapper.selectPage(pageParam, wrapper);

        List<CommentVO> vos = result.getRecords().stream()
                .map(c -> toVO(c, true))
                .toList();
        return PageResult.of(safePage, safeSize, result.getTotal(), vos);
    }

    @Override
    public void reviewComment(Long id, Integer status) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        if (comment.getStatus() != null && comment.getStatus().equals(status)) {
            throw new BusinessException(ResultCode.COMMENT_REVIEWED);
        }
        comment.setStatus(status);
        commentMapper.updateById(comment);
    }

    @Override
    public void deleteComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        // 逻辑删除（@TableLogic）
        commentMapper.deleteById(id);
    }

    // ── 树形组装（公开接口不暴露 email / ip） ──
    private List<CommentVO> buildTree(List<Comment> comments, boolean includeSensitive) {
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, CommentVO> voMap = new LinkedHashMap<>();
        for (Comment c : comments) {
            voMap.put(c.getId(), toVO(c, includeSensitive));
        }
        List<CommentVO> roots = new ArrayList<>();
        for (Comment c : comments) {
            CommentVO vo = voMap.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(vo);
            } else {
                CommentVO parent = voMap.get(c.getParentId());
                if (parent != null) {
                    if (parent.getReplies() == null) {
                        parent.setReplies(new ArrayList<>());
                    }
                    parent.getReplies().add(vo);
                } else {
                    // 父评论不存在（如被删），降级为顶级
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    private CommentVO toVO(Comment c, boolean includeSensitive) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setPostId(c.getPostId());
        vo.setParentId(c.getParentId());
        vo.setNickname(c.getNickname());
        vo.setContent(c.getContent());
        vo.setStatus(c.getStatus());
        vo.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : null);
        if (includeSensitive) {
            vo.setEmail(c.getEmail());
            vo.setIp(c.getIp());
        }
        return vo;
    }
}
