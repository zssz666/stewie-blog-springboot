package com.stewie.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 * <p>
 * 阶段 1：临时放行所有请求，保证开发期可用
 * 阶段 4：将改为 JWT 认证，区分公开 API 与管理 API
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 密码编码器，登录校验用
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（前后端分离无状态，不需要）
                .csrf(csrf -> csrf.disable())
                // 启用 CORS（使用 CorsFilter 的配置）
                .cors(cors -> {})
                // 无状态会话
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 放行所有请求（阶段 4 改为细粒度鉴权）
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                // 表单登录 / 默认登录页关闭
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
