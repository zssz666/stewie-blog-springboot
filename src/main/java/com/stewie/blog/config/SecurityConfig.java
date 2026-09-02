package com.stewie.blog.config;

import com.stewie.blog.security.JwtAuthenticationFilter;
import com.stewie.blog.security.RestAccessDeniedHandler;
import com.stewie.blog.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（阶段 4：JWT 认证）
 * <p>无状态会话 + JWT 过滤器；公开内容接口与登录接口，其余需登录。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

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
                // 前后端分离无状态，关闭 CSRF
                .csrf(csrf -> csrf.disable())
                // 启用 CORS
                .cors(cors -> {})
                // 无状态会话
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 登录接口公开
                        .requestMatchers("/api/auth/login").permitAll()
                        // 公开内容接口
                        .requestMatchers("/api/posts/**").permitAll()
                        .requestMatchers("/api/categories", "/api/tags", "/api/author").permitAll()
                        // 搜索接口公开
                        .requestMatchers("/api/search").permitAll()
                        // Sitemap（供 Bing / Google 爬取，含文章 slug）
                        .requestMatchers("/api/sitemap.xml").permitAll()
                        // 文章详情页 HTML（爬虫分流：Nginx 仅把爬虫转发到这里，人类走 SPA）
                        .requestMatchers("/post/**").permitAll()
                        // 登出公开（前端清除 token）
                        .requestMatchers("/api/auth/logout").permitAll()
                        // 文档与错误页
                        .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-resources/**", "/webjars/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 上传的封面图公开可读
                        .requestMatchers("/uploads/**").permitAll()
                        // 需要登录
                        .requestMatchers("/api/auth/me").authenticated()
                        // 其余一律需登录（为后续管理/写接口预留）
                        .anyRequest().authenticated())
                // 未登录/无权限返回 JSON
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // 不需要表单登录 / Basic
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // JWT 过滤器在用户名密码认证过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
