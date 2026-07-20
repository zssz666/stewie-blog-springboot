package com.stewie.blog.service;

import com.stewie.blog.dto.response.LoginResponse;
import com.stewie.blog.dto.vo.UserVO;

/**
 * 认证服务
 */
public interface AuthService {

    /**
     * 登录：校验用户名/密码，成功返回 JWT
     */
    LoginResponse login(String username, String password);

    /**
     * 获取当前登录用户（从 SecurityContext 取）
     */
    UserVO getCurrentUser();

    /**
     * 登出（JWT 无状态，服务端无需处理，前端清除 token 即可）
     */
    void logout();
}
