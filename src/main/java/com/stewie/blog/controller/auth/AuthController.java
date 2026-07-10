package com.stewie.blog.controller.auth;

import com.stewie.blog.common.Result;
import com.stewie.blog.dto.request.LoginRequest;
import com.stewie.blog.dto.response.LoginResponse;
import com.stewie.blog.dto.vo.UserVO;
import com.stewie.blog.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 API
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req.getUsername(), req.getPassword()));
    }

    /**
     * 获取当前登录用户（需携带有效 JWT）
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.getCurrentUser());
    }
}
