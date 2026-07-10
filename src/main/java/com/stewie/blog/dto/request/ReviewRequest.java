package com.stewie.blog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论审核请求（status: 1=通过 2=垃圾）
 */
@Data
public class ReviewRequest {

    @NotNull(message = "审核状态不能为空")
    private Integer status;
}
