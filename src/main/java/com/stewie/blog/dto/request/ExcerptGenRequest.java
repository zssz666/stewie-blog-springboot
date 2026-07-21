package com.stewie.blog.dto.request;

import lombok.Data;

/**
 * AI 摘要生成请求
 */
@Data
public class ExcerptGenRequest {

    /** 文章标题（可选，辅助生成更贴合的摘要） */
    private String title;

    /** 文章正文（HTML 源码，服务端会先抽成纯文本） */
    private String content;
}
