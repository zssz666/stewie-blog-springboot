package com.stewie.blog.dto.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录用户信息（对外暴露，不包含密码）
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    /** 状态：1=正常 0=禁用 */
    private Integer status;

    private LocalDateTime lastLoginAt;
}
