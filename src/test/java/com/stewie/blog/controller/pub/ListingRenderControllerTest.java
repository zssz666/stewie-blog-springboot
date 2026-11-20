package com.stewie.blog.controller.pub;

import com.stewie.blog.common.PageResult;
import com.stewie.blog.config.SecurityConfig;
import com.stewie.blog.dto.vo.PostVO;
import com.stewie.blog.security.*;
import com.stewie.blog.service.PostService;
import com.stewie.blog.service.PostService.PublishedPostFilter;
import com.stewie.blog.service.AuthorService;
import com.stewie.blog.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {ListingRenderController.class, ArticleRenderController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ListingRenderControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PostService posts;
    @MockitoBean AuthorService authors;
    @MockitoBean CommentService comments;
    @MockitoBean JwtUtil jwt;
    @MockitoBean UserDetailsServiceImpl users;

    private PostVO samplePost() {
        PostVO p = new PostVO();
        p.setSlug("published-example");
        p.setTitle("Published <example>");
        p.setExcerpt("An excerpt");
        return p;
    }

    @Test void anonymousHomeContainsLinksAndCanonical() throws Exception {
        when(posts.pagePublishedPosts(1, 5, new PublishedPostFilter(null, null, null)))
            .thenReturn(PageResult.of(1, 5, 1, List.of(samplePost())));
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(containsString("href=\"/post/published-example\"")))
            .andExpect(content().string(containsString("href=\"https://stewie.fun/\"")))
            .andExpect(content().string(containsString("Published &lt;example&gt;")));
    }

    @Test void secondPagePreservesFiltersAndHasCrawlablePagination() throws Exception {
        when(posts.pagePublishedPosts(2, 4, new PublishedPostFilter("Java", "Vue", 1)))
            .thenReturn(PageResult.of(2, 4, 12, List.of(samplePost())));
        mvc.perform(get("/articles").param("page", "2").param("category", "Java").param("tag", "Vue").param("postType", "1"))
            .andExpect(status().isOk())
            .andExpect(header().string("Vary", "User-Agent"))
            .andExpect(content().string(containsString("https://stewie.fun/articles?page=2&amp;category=Java&amp;tag=Vue&amp;postType=1")))
            .andExpect(content().string(containsString("href=\"/articles?category=Java&amp;tag=Vue&amp;postType=1\"")))
            .andExpect(content().string(containsString("href=\"/articles?page=3&amp;category=Java&amp;tag=Vue&amp;postType=1\"")));
    }

    @Test void invalidAndMissingPagesAreReal404() throws Exception {
        for (String value : List.of("0", "-1", "abc", "999999999999999999")) {
            mvc.perform(get("/articles").param("page", value)).andExpect(status().isNotFound());
        }
        verifyNoInteractions(posts);
        when(posts.pagePublishedPosts(2, 4, new PublishedPostFilter(null, null, null))).thenReturn(PageResult.empty(2, 4));
        mvc.perform(get("/articles").param("page", "2")).andExpect(status().isNotFound())
            .andExpect(header().string("X-Robots-Tag", "noindex"));
    }

    @Test void emptyFirstPageAndProtectedRoutes() throws Exception {
        when(posts.pagePublishedPosts(1, 4, new PublishedPostFilter(null, null, null))).thenReturn(PageResult.empty(1, 4));
        mvc.perform(get("/articles")).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("/post/"))));
        mvc.perform(get("/api/admin/posts")).andExpect(status().isUnauthorized());
        mvc.perform(post("/articles")).andExpect(status().isUnauthorized());
    }

    @Test void existingArticleEndpointStillRendersAndRejectsDrafts() throws Exception {
        PostVO published = samplePost();
        published.setStatus(1);
        published.setId(1L);
        published.setContent("<p>Published body</p>");
        when(posts.getPostBySlug("published-example")).thenReturn(published);
        when(comments.listApprovedTree(1L)).thenReturn(List.of());
        when(posts.getPopularPosts(5)).thenReturn(List.of());
        mvc.perform(get("/post/published-example")).andExpect(status().isOk())
            .andExpect(content().string(containsString("<p>Published body</p>")));
        PostVO draft = samplePost();
        draft.setStatus(0);
        when(posts.getPostBySlug("draft")).thenReturn(draft);
        mvc.perform(get("/post/draft")).andExpect(status().isNotFound())
            .andExpect(content().string(not(containsString("Published body"))));
        mvc.perform(get("/post/missing")).andExpect(status().isNotFound());
    }
}

