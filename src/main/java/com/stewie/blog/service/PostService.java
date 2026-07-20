package com.stewie.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stewie.blog.common.PageResult;
import com.stewie.blog.dto.request.PostRequest;
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
     * 根据 slug 查询文章详情（不修改浏览量，浏览量由独立埋点接口统计）
     */
    PostVO getPostBySlug(String slug);

    /**
     * 浏览量埋点：文章被打开时调用，views +1（仅对已发布且未删除文章生效）
     */
    void incrementViews(Long id);

    /**
     * 热门文章 Top N（按浏览量倒序）
     */
    List<PostVO> getPopularPosts(int limit);

    /**
     * 全文搜索已发布文章（ngram 中文分词），分页返回
     */
    PageResult<PostVO> searchPosts(String q, long page, long size);

    /**
     * 管理后台：分页查询全部文章（含草稿），可按状态过滤
     */
    PageResult<PostVO> adminPagePosts(long page, long size, Integer status);

    /**
     * 管理后台：按 id 获取单篇（编辑回显），不存在返回 null
     */
    PostVO getAdminPost(Long id);

    /**
     * 新建文章，返回新文章 id
     */
    Long createPost(PostRequest request);

    /**
     * 更新文章
     */
    void updatePost(Long id, PostRequest request);

    /**
     * 删除文章（逻辑删除）
     */
    void deletePost(Long id);
}
