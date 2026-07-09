package com.stewie.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stewie.blog.common.PageResult;
import com.stewie.blog.dto.vo.PostVO;
import com.stewie.blog.entity.Post;

import java.util.List;

public interface PostService extends IService<Post> {

    /**
     * 查询已发布文章列表（按发布日期倒序），含分类名与标签名
     */
    List<PostVO> listPublishedPosts();

    /**
     * 分页查询已发布文章（按发布日期倒序），含分类名与标签名
     *
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @param category 分类名（可选，传 null/空表示不过滤）
     * @param tag      标签名（可选，传 null/空表示不过滤）
     */
    PageResult<PostVO> pagePublishedPosts(long page, long size, String category, String tag);

    /**
     * 根据 slug 查询文章详情（浏览量 +1）
     */
    PostVO getPostBySlug(String slug);

    /**
     * 热门文章 Top N（按浏览量倒序）
     */
    List<PostVO> getPopularPosts(int limit);
}
