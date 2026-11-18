package com.stewie.blog.dto.request;

import com.stewie.blog.entity.Post;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 文章创建/更新请求（管理后台）
 */
@Data
public class PostRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    /** 可选 URL 路径（slug）；留空自动生成 */
    private String slug;

    private String excerpt;

    @NotBlank(message = "正文不能为空")
    private String content;

    /** 封面图 URL（上传接口返回，可空） */
    private String cover;

    private Long categoryId;

    @Min(value = Post.TYPE_QUICK_NOTE, message = "内容类型只能是 1（随手记）或 2（技术文章）")
    @Max(value = Post.TYPE_TECHNICAL_ARTICLE, message = "内容类型只能是 1（随手记）或 2（技术文章）")
    private Integer postType;

    /** 标签名列表（可空，自动建/关联） */
    private List<String> tags;

    /** 状态：0=草稿 1=已发布 */
    private Integer status;

    /** 发布日期 yyyy-MM-dd（可选） */
    private String publishDate;

    /** 阅读时长（分钟，可选；为空自动估算） */
    private Integer readingTime;
}
