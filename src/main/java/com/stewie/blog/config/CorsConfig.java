package com.stewie.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CORS 跨域配置
 * 默认允许前端开发服务器（localhost:5173）访问后端 API。
 * 部署到真实域名时，通过环境变量 CORS_ALLOWED_ORIGINS（逗号分隔）声明前端源，
 * 例如：CORS_ALLOWED_ORIGINS=https://stewie.github.io,https://blog.stewie.com
 * 若前后端同源（如 Nginx 同域反代），可不配置，同源不受 CORS 限制。
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:}")
    private String allowedOriginsEnv;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 基础白名单（开发/预览/部署）
        List<String> origins = new ArrayList<>(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                // 生产正式域名（已 ICP 备案 + HTTPS + Cloudflare）
                "https://stewie.fun",
                "https://www.stewie.fun",
                "http://stewie.fun",
                "http://www.stewie.fun"
        ));
        // 部署时通过环境变量追加真实前端域名
        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            for (String o : allowedOriginsEnv.split(",")) {
                String trimmed = o.trim();
                if (!trimmed.isEmpty()) origins.add(trimmed);
            }
        }
        config.setAllowedOrigins(origins);

        // 允许的请求方式
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 允许的请求头
        config.setAllowedHeaders(List.of("*"));
        // 暴露的响应头
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        // 是否携带 Cookie
        config.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
