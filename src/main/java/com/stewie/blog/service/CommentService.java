package com.stewie.blog.service;

import com.stewie.blog.common.PageResult;
import com.stewie.blog.dto.request.CommentRequest;
import com.stewie.blog.dto.vo.CommentVO;

import java.util.List;

/**
 * 评论服务
 */
public interface CommentService {

    /**
     * 公开：某文章已通过评论的树形结构（按时间升序）
     */
    List<CommentVO> listApprovedTree(Long postId);

    /**
     * 公开：提交评论（默认待审核 status=0）
     *
     * @return 新评论 ID
     */
    Long createComment(Long postId, CommentRequest req, String ip);

    /**
     * 管理：分页查询评论（可按文章 / 状态过滤）
     */
    PageResult<CommentVO> listForAdmin(Long postId, Integer status, long page, long size);

    /**
     * 管理：审核评论（设置状态：1=通过 2=垃圾）
     */
    void reviewComment(Long id, Integer status);

    /**
     * 管理：逻辑删除评论
     */
    void deleteComment(Long id);
}
