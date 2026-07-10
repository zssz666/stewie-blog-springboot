package com.stewie.blog.service;

import com.stewie.blog.dto.vo.LikeVO;

/**
 * 点赞服务（指纹去重）
 */
public interface LikeService {

    /**
     * 查询某文章当前指纹的点赞状态
     */
    LikeVO getStatus(Long postId, String fingerprint);

    /**
     * 点赞：若该指纹未赞则 +1 并落库；已赞则幂等返回当前状态
     */
    LikeVO like(Long postId, String fingerprint);
}
