package com.stewie.blog.service;

import java.util.List;

/**
 * IndexNow：内容变更时主动把 URL 推给搜索引擎，让爬虫立刻来抓，不必等 sitemap 轮询。
 * <p>一次推送会同步给所有参与方（Bing、Yandex、Seznam、Naver 等）。
 * 密钥文件需公开可访问：https://stewie.fun/{key}.txt（内容就是密钥本身）。
 * 官方文档：<a href="https://www.indexnow.org/documentation">indexnow.org</a></p>
 */
public interface IndexNowService {

    /** 推送结果 */
    record Result(boolean ok, int status, String message) {}

    /** 同步推送一批 URL */
    Result submit(List<String> urls);

    /** 异步推送一批 URL（后台操作不阻塞，失败只记日志） */
    void submitAsync(List<String> urls);

    /** 同步推送单篇文章（/post/{slug}） */
    Result submitPost(String slug);

    /** 异步推送单篇文章（/post/{slug}），供发布/更新/删除钩子调用 */
    void submitPostAsync(String slug);

    /** 批量推送全部已发布文章（首次接入或补推） */
    Result submitAllPublished();

    /** 是否启用（未配置 key 或手动关闭时返回 false，调用方跳过） */
    boolean isEnabled();
}
