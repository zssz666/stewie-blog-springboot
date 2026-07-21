package com.stewie.blog.service.impl;

import com.stewie.blog.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 摘要生成（腾讯 TokenHub 平台，OpenAI 兼容接口）
 * <p>默认模型 {@code deepseek-v4-flash-202605}（DeepSeek-V4 Flash）。
 * API Key 通过环境变量 {@code AI_API_KEY} </p>
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate = createRestTemplate();

    @Value("${ai.base-url:https://tokenhub.tencentmaas.com/v1}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:deepseek-v4-flash-202605}")
    private String model;

    /** 是否开启模型思考（推理）模式；默认关闭，响应更快更省 */
    @Value("${ai.enable-thinking:false}")
    private boolean enableThinking;

    /** 正文纯文本最大长度，避免超长上下文与额外 token 成本 */
    private static final int MAX_TEXT_LEN = 4000;

    @Override
    @SuppressWarnings("unchecked")
    public String generateExcerpt(String title, String htmlContent) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI 摘要功能未配置：请在环境变量/配置中设置 ai.api-key（AI_API_KEY）");
        }

        String text = htmlToText(htmlContent);
        if (text.isBlank()) {
            throw new IllegalArgumentException("正文内容为空，无法生成摘要");
        }
        if (text.length() > MAX_TEXT_LEN) {
            text = text.substring(0, MAX_TEXT_LEN);
        }

        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", """
                Role: 技术博客编辑
                Task: 提取标题与正文核心，输出单句高密度中文摘要卡片。
                Rules:
                1. 格式：≤70字纯文本，无前缀、换行或Markdown。
                2. 内容：禁复述标题！必提炼正文新信息（方案、链路、收益或踩坑）。
                3. 句式：强制“[具体动作/技术] + 解决[痛点] + 实现[收益]”结构。
                4. 禁用：引号/书名号/括号/标点(！？)；代词(本文/我)；弱动词(介绍/分享)；绝对副词(最)；Emoji。
                Workflow: 隐式提取 -> 填入句式 -> 剔除违禁词 -> 仅输出单句纯文本。
                Example:
                标题:一次Nginx502排查记录
                输出:优化超时配置与连接复用机制，解决服务端关闭upstream连接导致的偶发502故障"""
        ));
        messages.add(Map.of(
                "role", "user",
                "content", "标题：" + (title == null ? "" : title) + "\n正文：\n" + text
        ));
        reqBody.put("messages", messages);
        // deepseek-v4-flash 在 TokenHub 默认开启思考模式，推理会吃掉 max_tokens 预算，
        // 长文时 content 被挤空；显式关闭思考，content 直接产出、更快更省。
        if (!enableThinking) {
            reqBody.put("thinking", Map.of("type", "disabled"));
        }
        reqBody.put("temperature", 0.3);
        reqBody.put("max_tokens", 1024);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

        try {
            Map<String, Object> resp = restTemplate.postForObject(
                    baseUrl.replaceAll("/+$", "") + "/chat/completions", entity, Map.class);
            if (resp == null) {
                throw new IllegalStateException("AI 服务返回空响应");
            }
            // 腾讯云混元错误响应形如 { "error": { "message": "...", "code": "..." } }
            if (resp.containsKey("error")) {
                Object err = resp.get("error");
                String msg = err instanceof Map ? String.valueOf(((Map<?, ?>) err).get("message")) : String.valueOf(err);
                throw new IllegalStateException("AI 服务错误：" + msg);
            }
            Object choicesObj = resp.get("choices");
            if (!(choicesObj instanceof List) || ((List<?>) choicesObj).isEmpty()) {
                throw new IllegalStateException("AI 服务返回格式异常：" + resp);
            }
            List<?> choices = (List<?>) choicesObj;
            Object messageObj = ((Map<?, ?>) choices.get(0)).get("message");
            String content = messageObj instanceof Map
                    ? (String) ((Map<?, ?>) messageObj).get("content")
                    : null;
            if (content == null || content.isBlank() || "null".equals(content)) {
                // content 为空通常是开启了思考模式导致推理占满 max_tokens，记录原始响应便于排查
                Object finish = ((Map<?, ?>) choices.get(0)).get("finish_reason");
                log.error("AI 摘要 content 为空：finish_reason={}，raw={}", finish, resp);
                throw new IllegalStateException("AI 服务返回的摘要为空（finish_reason=" + finish + "）");
            }
            return content.trim();
        } catch (RuntimeException e) {
            // 网络异常 / 超时 / JSON 解析失败等
            log.error("调用 AI 摘要服务失败", e);
            throw new IllegalStateException("生成摘要失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将文章 HTML 转为纯文本：去标签、还原 HTML 实体、保留结构换行、压缩空行
     */
    private String htmlToText(String html) {
        if (html == null) return "";
        String text = html
                .replaceAll("(?i)<(br|/p|/div|/h[1-6]|/li|/tr|/section|/article|/blockquote)>", "\n")
                .replaceAll("(?i)<[^>]+>", "");
        text = HtmlUtils.htmlUnescape(text);
        text = text.replaceAll("[ \\t　]+", " ")
                .replaceAll("\n[ \\t　]+", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        return text;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }
}
