package com.stewie.blog.controller.pub;

import com.stewie.blog.dto.vo.PostVO;
import com.stewie.blog.service.PostService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 动态 Sitemap（供 Bing / Google 搜索引擎抓取）
 * <p>从数据库读取所有已发布文章 slug，生成含全部文章链接的 sitemap XML。
 * 前端 robots.txt 指向 /api/sitemap.xml（覆盖静态 public/sitemap.xml）。</p>
 */
@RestController
@RequestMapping("/api")
public class SitemapController {

    private final PostService postService;

    /** 站点基础 URL（与前端 useSeo.ts 的 SITE_URL 保持一致） */
    private static final String SITE_URL = "https://stewie.fun";

    /** ISO 8601 日期格式（sitemap lastmod 要求） */
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public SitemapController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder(2048);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1) 首页
        appendUrl(xml, SITE_URL + "/", "daily", "1.0");

        // 2) 文章列表
        appendUrl(xml, SITE_URL + "/articles", "weekly", "0.8");

        // 3) 关于页
        appendUrl(xml, SITE_URL + "/about", "monthly", "0.5");

        // 4) 所有已发布文章（核心：让搜索引擎发现每篇独立 URL）
        // ⚠ 路径必须与前端路由 router/index.ts 的 /post/:slug 一致（单数），
        //    写成 /posts/ 会导致搜索引擎抓到 404，文章全部无法收录
        List<PostVO> posts = postService.listPublishedPosts();
        for (PostVO post : posts) {
            if (post.getSlug() != null && !post.getSlug().isBlank()) {
                String loc = SITE_URL + "/post/" + post.getSlug();
                xml.append("  <url>\n");
                xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
                if (post.getDate() != null) {
                    xml.append("    <lastmod>").append(escapeXml(post.getDate())).append("</lastmod>\n");
                }
                xml.append("    <changefreq>monthly</changefreq>\n");
                xml.append("    <priority>0.7</priority>\n");
                xml.append("  </url>\n");
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private static void appendUrl(StringBuilder xml, String loc, String changefreq, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    /** 最小转义，防止 URL 中的 & 等字符破坏 XML 结构 */
    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
}
