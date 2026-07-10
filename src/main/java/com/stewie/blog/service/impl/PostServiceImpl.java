package com.stewie.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.PageResult;
import com.stewie.blog.common.ResultCode;
import com.stewie.blog.dto.request.PostRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 100);

        Long categoryId = null;
        if (category != null && !category.isBlank()) {
            Category cat = categoryMapper.selectOne(
                    new LambdaQueryWrapper<Category>().eq(Category::getName, category));
            if (cat == null) {
                return PageResult.of(safePage, safeSize, 0, Collections.emptyList());
            }
            categoryId = cat.getId();
        }

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

    /* ── 管理后台 ── */

    @Override
    public PageResult<PostVO> adminPagePosts(long page, long size, Integer status) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 100);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>();
        if (status != null) {
            wrapper.eq(Post::getStatus, status);
        }
        wrapper.orderByDesc(Post::getCreatedAt);
        Page<Post> pageParam = new Page<>(safePage, safeSize);
        Page<Post> result = page(pageParam, wrapper);
        return PageResult.of(safePage, safeSize, result.getTotal(), convertPosts(result.getRecords()));
    }

    @Override
    public PostVO getAdminPost(Long id) {
        Post post = getById(id);
        if (post == null) {
            return null;
        }
        return convertPosts(List.of(post)).stream().findFirst().orElse(null);
    }

    @Override
    public Long createPost(PostRequest req) {
        Integer status = req.getStatus() == null ? 0 : req.getStatus();
        Post post = new Post();
        post.setTitle(req.getTitle());
        post.setSlug(generateSlug(req.getTitle(), req.getSlug()));
        post.setExcerpt(req.getExcerpt());
        post.setContent(req.getContent());
        post.setCover(req.getCover());
        post.setCategoryId(req.getCategoryId());
        post.setStatus(status);
        post.setPublishDate(parseDate(req.getPublishDate()));
        post.setPublishedAt(status == 1 ? LocalDateTime.now() : null);
        post.setReadingTime(req.getReadingTime() != null ? req.getReadingTime() : estimateReadingTime(req.getContent()));
        post.setViews(0L);
        post.setLikes(0L);
        save(post);
        syncTags(post.getId(), req.getTags());
        return post.getId();
    }

    @Override
    public void updatePost(Long id, PostRequest req) {
        Post post = getById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        // slug：若显式提供且合法且与现 slug 不同，则重新生成唯一 slug
        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            String s = req.getSlug().trim();
            if (s.matches("[a-zA-Z0-9_-]+") && !s.equals(post.getSlug())) {
                post.setSlug(ensureUniqueSlug(s, id));
            }
        }
        post.setTitle(req.getTitle());
        post.setExcerpt(req.getExcerpt());
        post.setContent(req.getContent());
        post.setCover(req.getCover());
        post.setCategoryId(req.getCategoryId());
        Integer status = req.getStatus() == null ? post.getStatus() : req.getStatus();
        if (status == 1 && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        if (status == 0) {
            post.setPublishedAt(null);
        }
        post.setStatus(status);
        if (req.getPublishDate() != null) {
            post.setPublishDate(parseDate(req.getPublishDate()));
        }
        post.setReadingTime(req.getReadingTime() != null ? req.getReadingTime() : estimateReadingTime(req.getContent()));
        updateById(post);
        syncTags(id, req.getTags());
    }

    @Override
    public void deletePost(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
    }

    /* ── 私有工具 ── */

    private String generateSlug(String title, String provided) {
        if (provided != null && !provided.isBlank() && provided.trim().matches("[a-zA-Z0-9_-]+")) {
            return ensureUniqueSlug(provided.trim(), null);
        }
        String auto = "post-" + Long.toString(System.currentTimeMillis(), 36)
                + "-" + UUID.randomUUID().toString().substring(0, 4);
        return ensureUniqueSlug(auto, null);
    }

    private String ensureUniqueSlug(String base, Long excludeId) {
        String slug = base;
        int i = 2;
        while (true) {
            LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<Post>().eq(Post::getSlug, slug);
            if (excludeId != null) {
                w.ne(Post::getId, excludeId);
            }
            if (count(w) == 0) {
                return slug;
            }
            slug = base + "-" + i++;
        }
    }

    private void syncTags(Long postId, List<String> tagNames) {
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostId, postId));
        if (tagNames == null) {
            return;
        }
        for (String name : tagNames) {
            String n = name.trim();
            if (n.isEmpty()) {
                continue;
            }
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, n));
            if (tag == null) {
                tag = new Tag();
                tag.setName(n);
                tag.setSlug(slugify(n));
                tagMapper.insert(tag);
            }
            PostTag pt = new PostTag();
            pt.setPostId(postId);
            pt.setTagId(tag.getId());
            postTagMapper.insert(pt);
        }
    }

    private String slugify(String name) {
        String s = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return s.isEmpty() ? ("tag-" + System.currentTimeMillis()) : s;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private int estimateReadingTime(String content) {
        if (content == null || content.isEmpty()) {
            return 1;
        }
        String text = content.replaceAll("<[^>]+>", "");
        return Math.max(1, (int) Math.ceil(text.length() / 350.0));
    }

    /**
     * 批量组装 VO：关联分类名 + 全部标签名（并提供首个标签做向后兼容）
     */
    private List<PostVO> convertPosts(List<Post> posts) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();

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

        List<PostTag> postTags = postTagMapper.selectList(
                new LambdaQueryWrapper<PostTag>().in(PostTag::getPostId, postIds));
        Map<Long, List<Long>> postTagIds = new LinkedHashMap<>();
        for (PostTag pt : postTags) {
            postTagIds.computeIfAbsent(pt.getPostId(), k -> new ArrayList<>()).add(pt.getTagId());
        }
        Set<Long> tagIds = new HashSet<>(postTagIds.values().stream()
                .flatMap(List::stream).collect(Collectors.toSet()));
        Map<Long, String> tagNameMap = tagIds.isEmpty()
                ? Collections.emptyMap()
                : tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, tagIds))
                .stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));

        Map<Long, List<String>> postTagsMap = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Long>> e : postTagIds.entrySet()) {
            List<String> names = e.getValue().stream()
                    .map(tagNameMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            postTagsMap.put(e.getKey(), names);
        }

        return posts.stream().map(p -> {
            PostVO vo = new PostVO();
            vo.setId(p.getId());
            vo.setSlug(p.getSlug());
            vo.setTitle(p.getTitle());
            vo.setExcerpt(p.getExcerpt());
            vo.setContent(p.getContent());
            vo.setCover(p.getCover());
            vo.setCoverColor(p.getCoverColor());
            vo.setCategory(p.getCategoryId() != null ? categoryNameMap.get(p.getCategoryId()) : null);
            List<String> tags = postTagsMap.getOrDefault(p.getId(), List.of());
            vo.setTags(tags);
            vo.setTag(tags.isEmpty() ? null : tags.get(0));
            vo.setDate(p.getPublishDate() != null ? p.getPublishDate().toString() : null);
            vo.setReadingTime(p.getReadingTime());
            vo.setViews(p.getViews());
            vo.setLikes(p.getLikes());
            vo.setStatus(p.getStatus());
            return vo;
        }).toList();
    }
}
