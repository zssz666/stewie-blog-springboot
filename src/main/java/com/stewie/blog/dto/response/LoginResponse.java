package com.stewie.blog.dto.response;

import lombok.Data;

/**
 * 登录响应：返回 JWT 与基本信息
 */
@Data
public class LoginResponse {

    /** JWT 令牌 */
    private String token;

    /** 令牌类型，固定 Bearer */
    private String tokenType = "Bearer";

    /** 有效时长（毫秒） */
    private long expiresIn;

    /** 当前登录用户 */
    private com.stewie.blog.dto.vo.UserVO user;
}
