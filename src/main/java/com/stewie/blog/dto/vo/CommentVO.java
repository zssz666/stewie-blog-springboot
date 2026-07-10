package com.stewie.blog.dto.vo;

import lombok.Data;

import java.util.List;

/**
 * 评论视图对象（树形：replies 为子评论）
 * 公开接口不填充 email / ip（不公开），管理接口会填充。
 */
@Data
public class CommentVO {

    private Long id;
    private Long postId;
    private Long parentId;
    private String nickname;
    private String content;
    private Integer status;

    /** 公开接口为 null，不泄露 */
    private String email;
    /** 公开接口为 null */
    private String ip;

    /** 格式化后的创建时间 yyyy-MM-dd HH:mm */
    private String createdAt;

    /** 子评论（楼中楼），顶级评论才有 */
    private List<CommentVO> replies;
}
