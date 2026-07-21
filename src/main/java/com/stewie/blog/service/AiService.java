package com.stewie.blog.service;

/**
 * AI 能力（摘要生成等）
 */
public interface AiService {

    /**
     * 根据标题与正文 HTML 生成一句话摘要
     *
     * @param title       文章标题（可空）
     * @param htmlContent 正文 HTML 源码
     * @return 摘要文本（已 trim，无包裹符号）
     */
    String generateExcerpt(String title, String htmlContent);
}
