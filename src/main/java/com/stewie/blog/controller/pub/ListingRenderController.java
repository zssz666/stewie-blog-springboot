package com.stewie.blog.controller.pub;

import com.stewie.blog.common.PageResult;
import com.stewie.blog.dto.vo.PostVO;
import com.stewie.blog.entity.Post;
import com.stewie.blog.service.PostService;
import com.stewie.blog.service.PostService.PublishedPostFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

/** Public HTML entry points; use the same published data and page sizes as the SPA. */
@Controller
public class ListingRenderController {
    private final PostService posts;

    public ListingRenderController(PostService posts) {
        this.posts = posts;
    }

    @GetMapping(value = "/", produces = "text/html")
    public String home(Model model, HttpServletResponse response) {
        return render(true, 1, new PublishedPostFilter(null, null, null), model, response);
    }

    @GetMapping(value = "/articles", produces = "text/html")
    public String articles(@RequestParam(defaultValue = "1") String page,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String tag,
                           @RequestParam(required = false) String postType,
                           Model model, HttpServletResponse response) {
        long number;
        Integer type;
        try {
            number = Long.parseLong(page);
            type = postType == null ? null : Integer.valueOf(postType);
        } catch (NumberFormatException e) {
            return missing(model, response);
        }
        if (number < 1 || number > Integer.MAX_VALUE || (type != null && !Post.isValidPostType(type))) {
            return missing(model, response);
        }
        return render(false, number, new PublishedPostFilter(category, tag, type), model, response);
    }

    private String render(boolean home, long page, PublishedPostFilter filter,
                          Model model, HttpServletResponse response) {
        PageResult<PostVO> result = posts.pagePublishedPosts(page, home ? 5 : 4, filter);
        if (page > 1 && page > result.getPages()) return missing(model, response);
        String section = switch (filter.postType() == null ? 0 : filter.postType()) {
            case Post.TYPE_QUICK_NOTE -> "随手记";
            case Post.TYPE_TECHNICAL_ARTICLE -> "技术文章";
            default -> "全部实验报告";
        };
        model.addAttribute("posts", result.getList());
        model.addAttribute("title", home ? "Stewie 的实验室 — 开发中的踩坑实验报告" : section + " · 第 " + page + " 页 | Stewie 的实验室");
        model.addAttribute("description", "Vue、TypeScript、Java 开发记录与学习心得，查看最新文章和实验报告。");
        model.addAttribute("heading", home ? "把踩过的坑，写成实验报告。" : section);
        model.addAttribute("canonical", "https://stewie.fun" + (home ? "/" : listingUrl(page, filter)));
        model.addAttribute("previous", !home && page > 1 ? listingUrl(page - 1, filter) : null);
        model.addAttribute("next", !home && page < result.getPages() ? listingUrl(page + 1, filter) : null);
        model.addAttribute("page", page);
        model.addAttribute("home", home);
        response.setHeader("Cache-Control", "private, no-cache, must-revalidate");
        response.setHeader("Vary", "User-Agent");
        return "listing";
    }

    private String listingUrl(long page, PublishedPostFilter filter) {
        var url = UriComponentsBuilder.fromPath("/articles");
        if (page > 1) url.queryParam("page", page);
        if (filter.category() != null && !filter.category().isBlank()) url.queryParam("category", filter.category());
        if (filter.tag() != null && !filter.tag().isBlank()) url.queryParam("tag", filter.tag());
        if (filter.postType() != null) url.queryParam("postType", filter.postType());
        return url.build().encode().toUriString();
    }

    private String missing(Model model, HttpServletResponse response) {
        response.setStatus(404);
        response.setHeader("X-Robots-Tag", "noindex");
        model.addAttribute("siteName", "Stewie 的实验室");
        model.addAttribute("siteUrl", "https://stewie.fun");
        return "article-404";
    }
}
