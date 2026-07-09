package com.stewie.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者信息（对应 t_author）
 */
@Data
@TableName("t_author")
public class Author {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String role;
    private String bio;
    private String avatar;
    /** 技能数组存为 JSON 字符串，如 ["Vue 3","TypeScript"] */
    private String skills;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
