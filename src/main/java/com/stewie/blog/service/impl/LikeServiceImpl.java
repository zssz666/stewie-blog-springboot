package com.stewie.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.ResultCode;
import com.stewie.blog.dto.vo.LikeVO;
import com.stewie.blog.entity.LikeRecord;
import com.stewie.blog.entity.Post;
import com.stewie.blog.mapper.LikeRecordMapper;
import com.stewie.blog.mapper.PostMapper;
import com.stewie.blog.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 点赞服务实现（指纹去重 + 同步 t_post.likes）
 */
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRecordMapper likeRecordMapper;
    private final PostMapper postMapper;

    @Override
    public LikeVO getStatus(Long postId, String fingerprint) {
        Post post = requirePost(postId);
        boolean liked = likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getPostId, postId)
                .eq(LikeRecord::getFingerprint, fingerprint)) > 0;
        return new LikeVO(liked, safeLikes(post.getLikes()));
    }

    @Override
    public LikeVO like(Long postId, String fingerprint) {
        Post post = requirePost(postId);

        Long existing = likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getPostId, postId)
                .eq(LikeRecord::getFingerprint, fingerprint));
        if (existing != null && existing > 0) {
            // 已点赞：幂等返回当前状态，不重复 +1
            return new LikeVO(true, safeLikes(post.getLikes()));
        }

        LikeRecord record = new LikeRecord();
        record.setPostId(postId);
        record.setFingerprint(fingerprint);
        record.setCreatedAt(LocalDateTime.now());
        likeRecordMapper.insert(record);

        long newLikes = safeLikes(post.getLikes()) + 1;
        Post update = new Post();
        update.setId(postId);
        update.setLikes(newLikes);
        postMapper.updateById(update);

        return new LikeVO(true, newLikes);
    }

    private Post requirePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        return post;
    }

    private long safeLikes(Long likes) {
        return likes == null ? 0L : likes;
    }
}
