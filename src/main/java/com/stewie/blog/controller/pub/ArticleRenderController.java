package com.stewie.blog.controller.pub;

import com.stewie.blog.dto.vo.AuthorVO;
import com.stewie.blog.dto.vo.CommentVO;
import com.stewie.blog.dto.vo.PostVO;
import com.stewie.blog.service.AuthorService;
import com.stewie.blog.service.CommentService;
import com.stewie.blog.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章详情页 HTML 渲染（爬虫分流 / Dynamic Rendering）。
 * <p>仅搜索引擎与 AI 爬虫访问此端点：Nginx 依据 User-Agent 把爬虫转发到 /post/{slug}，
 * 由本控制器查文章 → 套 Thymeleaf 模板 → 吐完整 HTML 文档（正文 + SEO meta + JSON-LD + 评论）。
 * 普通用户访问 /post/{slug} 时由 Nginx 直接返回 SPA 的 index.html，体验不变。</p>
 * <p>爬虫与用户所见主内容同源同数据（同一 post.content / excerpt / 评论树 / canonical），
 * 属 Google 认可的 Dynamic Rendering，非 cloaking。</p>
 */
@Controller
public class ArticleRenderController {

    private static final String SITE_URL = "https://stewie.fun";
    private static final String SITE_NAME = "Stewie 的前端实验室";

    private final PostService postService;
    private final AuthorService authorService;
    private final CommentService commentService;
    private final ObjectMapper objectMapper;

    public ArticleRenderController(PostService postService,
                                   AuthorService authorService,
                                   CommentService commentService,
                                   ObjectMapper objectMapper) {
        this.postService = postService;
        this.authorService = authorService;
        this.commentService = commentService;
        this.objectMapper = objectMapper;
    }

    /**
     * 爬虫专用：返回完整渲染的文章 HTML。
     * 注意：不抛异常做 404（全局 @RestControllerAdvice 会兜成 200 JSON 软 404），
     * 而是手动 setStatus(404) + 渲染 article-404 模板。
     */
    @GetMapping(value = "/post/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public String article(@PathVariable String slug, Model model, HttpServletResponse response) {
        PostVO post = postService.getPostBySlug(slug);
        // 仅已发布文章才渲染；草稿 / 不存在 → 真实 404
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("siteName", SITE_NAME);
            model.addAttribute("siteUrl", SITE_URL);
            model.addAttribute("slug", slug);
            applyCachePolicy(response);
            return "article-404";
        }

        AuthorVO author = authorService.getAuthor();
        List<CommentVO> comments = commentService.listApprovedTree(post.getId());
        List<PostVO> popular = postService.getPopularPosts(5);

        model.addAttribute("post", post);
        model.addAttribute("author", author);
        model.addAttribute("comments", comments);
        model.addAttribute("popular", popular);
        model.addAttribute("siteUrl", SITE_URL);
        model.addAttribute("siteName", SITE_NAME);
        model.addAttribute("canonical", SITE_URL + "/post/" + slug);
        // og:image 必须绝对 URL：cover 若是 http(s) 开头直接用，否则拼站点根
        String coverAbs = resolveAbsoluteUrl(post.getCover());
        model.addAttribute("coverAbs", coverAbs);
        // JSON-LD BlogPosting：用 Jackson 安全序列化，避免手拼 JSON 的转义坑
        model.addAttribute("blogPostingJsonLd", buildBlogPostingJsonLd(post, author, coverAbs, SITE_URL + "/post/" + slug));

        applyCachePolicy(response);
        return "article";
    }

    /**
     * 缓存策略：private + no-cache，确保 Cloudflare 不缓存 /post/* 的爬虫 HTML
     * （避免同一 URL 不同 UA 命中错配变体，导致人类拿到爬虫 HTML 或反之）。
     * 爬虫突发由 Nginx proxy_cache（按 URI+UA 键）扛。
     * Vary: User-Agent 作为纵深防御。
     */
    private void applyCachePolicy(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-cache, must-revalidate");
        response.setHeader("Vary", "User-Agent");
    }

    private String resolveAbsoluteUrl(String cover) {
        if (cover == null || cover.isBlank()) {
            return SITE_URL + "/og-image.png";
        }
        return cover.startsWith("http") ? cover : SITE_URL + (cover.startsWith("/") ? "" : "/") + cover;
    }

    /** 构造 BlogPosting 结构化数据 JSON 字符串（供模板 th:utext 注入 <script type=ld+json>） */
    private String buildBlogPostingJsonLd(PostVO post, AuthorVO author, String coverAbs, String canonical) {
        Map<String, Object> authorNode = new LinkedHashMap<>();
        authorNode.put("@type", "Person");
        authorNode.put("name", author != null && author.getName() != null ? author.getName() : "Stewie");
        authorNode.put("url", SITE_URL + "/about");

        Map<String, Object> jsonLd = new LinkedHashMap<>();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "BlogPosting");
        jsonLd.put("headline", post.getTitle());
        jsonLd.put("description", post.getExcerpt());
        jsonLd.put("datePublished", post.getDate());
        jsonLd.put("dateModified", post.getDate());
        jsonLd.put("url", canonical);
        jsonLd.put("image", coverAbs);
        jsonLd.put("mainEntityOfPage", Map.of("@type", "WebPage", "@id", canonical));
        jsonLd.put("author", authorNode);
        Map<String, Object> publisher = new LinkedHashMap<>(authorNode);
        publisher.put("@id", SITE_URL + "/#person");
        jsonLd.put("publisher", publisher);
        try {
            return objectMapper.writeValueAsString(jsonLd);
        } catch (Exception e) {
            return "{}";
        }
    }
}
