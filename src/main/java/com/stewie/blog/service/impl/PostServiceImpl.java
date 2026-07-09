package com.stewie.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stewie.blog.common.PageResult;
import com.stewie.blog.dto.vo.PostVO;
import com.stewie.blog.entity.Category;
import com.stewie.blog.entity.Post;
import com.stewie.blog.entity.PostTag;
import com.stewie.blog.entity.Tag;
import com.stewie.blog.mapper.CategoryMapper;
import com.stewie.blog.mapper.PostMapper;
import com.stewie.blog.mapper.PostTagMapper;
import com.stewie.blog.mapper.TagMapper;
import com.stewie.blog.service.PostService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;

    public PostServiceImpl(CategoryMapper categoryMapper, TagMapper tagMapper, PostTagMapper postTagMapper) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.postTagMapper = postTagMapper;
    }

    @Override
    public List<PostVO> listPublishedPosts() {
        List<Post> posts = list(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .orderByDesc(Post::getPublishDate));
        return convertPosts(posts);
    }

    @Override
    public PageResult<PostVO> pagePublishedPosts(long page, long size, String category, String tag) {
        // 防御性纠正非法入参
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 100);

        // 分类名 -> categoryId（按名称过滤）
        Long categoryId = null;
        if (category != null && !category.isBlank()) {
            Category cat = categoryMapper.selectOne(
                    new LambdaQueryWrapper<Category>().eq(Category::getName, category));
            if (cat == null) {
                // 分类不存在 -> 无匹配文章
                return PageResult.of(safePage, safeSize, 0, Collections.emptyList());
            }
            categoryId = cat.getId();
        }

        // 标签名 -> 命中该标签的文章 id 集合（多对多，走 post_tag 关联表）
        Set<Long> tagPostIds = null;
        if (tag != null && !tag.isBlank()) {
            Tag t = tagMapper.selectOne(
                    new LambdaQueryWrapper<Tag>().eq(Tag::getName, tag));
            if (t == null) {
                return PageResult.of(safePage, safeSize, 0, Collections.emptyList());
            }
            List<PostTag> ptList = postTagMapper.selectList(
                    new LambdaQueryWrapper<PostTag>().eq(PostTag::getTagId, t.getId()));
            tagPostIds = ptList.stream().map(PostTag::getPostId).collect(Collectors.toSet());
            if (tagPostIds.isEmpty()) {
                return PageResult.of(safePage, safeSize, 0, Collections.emptyList());
            }
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .orderByDesc(Post::getPublishDate);
        if (categoryId != null) {
            wrapper.eq(Post::getCategoryId, categoryId);
        }
        if (tagPostIds != null) {
            wrapper.in(Post::getId, tagPostIds);
        }

        Page<Post> pageParam = new Page<>(safePage, safeSize);
        Page<Post> result = page(pageParam, wrapper);

        List<PostVO> vos = convertPosts(result.getRecords());
        return PageResult.of(safePage, safeSize, result.getTotal(), vos);
    }

    @Override
    public PostVO getPostBySlug(String slug) {
        Post post = getOne(new LambdaQueryWrapper<Post>()
                .eq(Post::getSlug, slug)
                .eq(Post::getStatus, 1));
        if (post == null) {
            return null;
        }
        // 浏览量 +1
        Post update = new Post();
        update.setId(post.getId());
        update.setViews(post.getViews() + 1);
        updateById(update);
        post.setViews(post.getViews() + 1);

        List<PostVO> vos = convertPosts(List.of(post));
        return vos.isEmpty() ? null : vos.get(0);
    }

    @Override
    public List<PostVO> getPopularPosts(int limit) {
        List<Post> posts = list(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .orderByDesc(Post::getViews)
                .last("LIMIT " + limit));
        return convertPosts(posts);
    }

    /**
     * 批量组装 VO：关联分类名 + 标签名（每篇取首个标签，兼容前端单标签字段）
     */
    private List<PostVO> convertPosts(List<Post> posts) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        // 分类名
        Set<Long> categoryIds = posts.stream()
                .map(Post::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .in(Category::getId, categoryIds))
                        .stream()
                        .collect(Collectors.toMap(Category::getId, Category::getName));

        // 标签名（每篇取首个）
        List<PostTag> postTags = postTagMapper.selectList(
                new LambdaQueryWrapper<PostTag>().in(PostTag::getPostId, postIds));
        Map<Long, Long> postFirstTagId = new LinkedHashMap<>();
        for (PostTag pt : postTags) {
            postFirstTagId.putIfAbsent(pt.getPostId(), pt.getTagId());
        }
        Set<Long> tagIds = new HashSet<>(postFirstTagId.values());
        Map<Long, String> tagNameMap = tagIds.isEmpty()
                ? Collections.emptyMap()
                : tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, tagIds))
                        .stream()
                        .collect(Collectors.toMap(Tag::getId, Tag::getName));

        return posts.stream().map(p -> {
            PostVO vo = new PostVO();
            vo.setId(p.getId());
            vo.setSlug(p.getSlug());
            vo.setTitle(p.getTitle());
            vo.setExcerpt(p.getExcerpt());
            vo.setContent(p.getContent());
            vo.setCoverColor(p.getCoverColor());
            vo.setCategory(p.getCategoryId() != null ? categoryNameMap.get(p.getCategoryId()) : null);
            Long tagId = postFirstTagId.get(p.getId());
            vo.setTag(tagId != null ? tagNameMap.get(tagId) : null);
            vo.setDate(p.getPublishDate() != null ? p.getPublishDate().toString() : null);
            vo.setReadingTime(p.getReadingTime());
            vo.setViews(p.getViews());
            vo.setLikes(p.getLikes());
            return vo;
        }).toList();
    }
}
