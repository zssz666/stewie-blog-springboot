package com.stewie.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stewie.blog.dto.vo.DashboardStatsVO;
import com.stewie.blog.entity.Comment;
import com.stewie.blog.entity.Post;
import com.stewie.blog.mapper.CommentMapper;
import com.stewie.blog.mapper.PostMapper;
import com.stewie.blog.service.StatsService;
import org.springframework.stereotype.Service;

@Service
public class StatsServiceImpl implements StatsService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    public StatsServiceImpl(PostMapper postMapper, CommentMapper commentMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public DashboardStatsVO getDashboardStats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        // @TableLogic 自动过滤 deleted=0
        vo.setPostsTotal(postMapper.selectCount(new LambdaQueryWrapper<Post>()));
        vo.setPostsPublished(postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 1)));
        vo.setCommentsTotal(commentMapper.selectCount(new LambdaQueryWrapper<Comment>()));
        vo.setCommentsPending(commentMapper.countPending());
        vo.setViewsTotal(postMapper.sumViews());
        vo.setLikesTotal(postMapper.sumLikes());
        return vo;
    }
}
