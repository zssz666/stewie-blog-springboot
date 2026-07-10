package com.stewie.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体（对应 t_comment，支持楼中楼）
 */
@Data
@TableName("t_comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    /** 父评论 ID，顶级评论为 null */
    private Long parentId;

    private String nickname;

    /** 邮箱不对外公开 */
    private String email;

    private String content;

    /** 状态：0=待审核 1=已通过 2=垃圾 */
    private Integer status;

    /** 评论者 IP */
    private String ip;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
}
