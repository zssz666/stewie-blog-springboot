package com.stewie.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stewie.blog.entity.Post;
import com.stewie.blog.mapper.PostMapper;
import com.stewie.blog.service.IndexNowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * IndexNow 实现：向 api.indexnow.org POST 待抓取的 URL 列表。
 * <p>单次最多 10000 条 URL。响应语义（见 https://www.indexnow.org/documentation）：
 * <ul>
 *   <li>200 已受理并处理（正常成功）</li>
 *   <li>202 已接收，密钥校验待完成（首次提交常见；校验通过后后续返回 200）</li>
 *   <li>400 请求格式错误 / 403 密钥无效（密钥文件 404 或内容不匹配）/ 422 URL 不属于该 host / 429 限流</li>
 * </ul>
 * 200 与 202 均视为成功，其余仅记录日志，失败不影响主流程（发布/更新照常完成）。</p>
 */
@Slf4j
@Service
public class IndexNowServiceImpl implements IndexNowService {

    /** IndexNow 单次请求 URL 上限 */
    private static final int MAX_URLS = 10_000;

    private final PostMapper postMapper;
    private final RestTemplate restTemplate;

    @Value("${indexnow.enabled:false}")
    private boolean enabled;

    @Value("${indexnow.host:}")
    private String host;

    @Value("${indexnow.site-url:}")
    private String siteUrl;

    @Value("${indexnow.key:}")
    private String key;

    @Value("${indexnow.key-location:}")
    private String keyLocation;

    @Value("${indexnow.endpoint:https://api.indexnow.org/indexnow}")
    private String endpoint;

    public IndexNowServiceImpl(PostMapper postMapper) {
        this.postMapper = postMapper;
        this.restTemplate = createRestTemplate();
    }

    @Override
    public Result submit(List<String> urls) {
        if (!enabled) {
            return new Result(false, 0, "IndexNow 未启用（indexnow.enabled=false）");
        }
        List<String> list = (urls == null ? List.<String>of() : urls).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (list.isEmpty()) {
            return new Result(false, 0, "URL 列表为空，跳过推送");
        }
        if (key == null || key.isBlank()) {
            log.warn("IndexNow 未配置 indexnow.key，跳过推送");
            return new Result(false, 0, "未配置 indexnow.key");
        }
        if (list.size() > MAX_URLS) {
            list = list.subList(0, MAX_URLS);
            log.warn("IndexNow URL 超过 {} 条，已截断", MAX_URLS);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("host", host);
        body.put("key", key);
        body.put("keyLocation", keyLocation);
        body.put("urlList", list);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/json;charset=UTF-8"));

        try {
            ResponseEntity<String> resp =
                    restTemplate.postForEntity(endpoint, new HttpEntity<>(body, headers), String.class);
            int status = resp.getStatusCode().value();
            boolean ok = status == 200 || status == 202;
            // IndexNow 语义：200=已受理并处理；202=已接收，密钥校验待完成（首次提交常见，
            // 校验通过后后续提交通常转 200）。两者都算成功，但 202 值得提示排查密钥文件。
            String hint = switch (status) {
                case 200 -> "已受理";
                case 202 -> "已接收，密钥校验中（确认 https://" + host + "/" + key + ".txt 可公开访问，校验通过后后续将返回 200）";
                default -> "未受理";
            };
            if (ok) {
                log.info("IndexNow 推送 {} 条 URL → HTTP {} {}", list.size(), status, hint);
            } else {
                log.warn("IndexNow 推送 {} 条 URL → HTTP {} {} body={}", list.size(), status, hint, resp.getBody());
            }
            return new Result(ok, status, ok ? "已推送 " + list.size() + " 条 URL（" + hint + "）" : "HTTP " + status);
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            log.warn("IndexNow 推送被拒绝：HTTP {} {}", status, e.getResponseBodyAsString());
            return new Result(false, status, "HTTP " + status + " " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.warn("IndexNow 推送失败：{}", e.getMessage());
            return new Result(false, 0, "请求失败：" + e.getMessage());
        }
    }

    @Override
    public void submitAsync(List<String> urls) {
        if (!enabled) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                submit(urls);
            } catch (Exception e) {
                log.warn("IndexNow 异步推送异常：{}", e.getMessage());
            }
        });
    }

    @Override
    public Result submitPost(String slug) {
        return submit(List.of(buildPostUrl(slug)));
    }

    @Override
    public void submitPostAsync(String slug) {
        if (!enabled || slug == null || slug.isBlank()) {
            return;
        }
        submitAsync(List.of(buildPostUrl(slug)));
    }

    @Override
    public Result submitAllPublished() {
        // 直接查 Mapper 而非 PostService，避免与 PostServiceImpl 形成循环依赖
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .isNotNull(Post::getSlug));
        List<String> urls = posts.stream()
                .map(Post::getSlug)
                .filter(s -> s != null && !s.isBlank())
                .map(this::buildPostUrl)
                .distinct()
                .toList();
        log.info("IndexNow 批量推送：已发布文章 {} 篇", urls.size());
        return submit(urls);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** 组装文章详情页 URL：{siteUrl}/post/{slug}（路径须与前端路由/canonical 一致） */
    private String buildPostUrl(String slug) {
        String base = (siteUrl != null && siteUrl.endsWith("/"))
                ? siteUrl.substring(0, siteUrl.length() - 1)
                : siteUrl;
        return base + "/post/" + slug;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
