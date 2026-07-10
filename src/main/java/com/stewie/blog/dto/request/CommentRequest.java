package com.stewie.blog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交评论请求
 */
@Data
public class CommentRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称不能超过 50 字")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过 500 字")
    private String content;

    /** 回复的父评论 ID（顶级评论为 null） */
    private Long parentId;
}
