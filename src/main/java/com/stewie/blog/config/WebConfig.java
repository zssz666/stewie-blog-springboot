package com.stewie.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

/**
 * 静态资源配置：将上传的封面图通过 /uploads/** 对外提供访问
 * <p>上传目录由配置 upload.path 指定（默认 jar 同级的 ./uploads），生产建议用环境变量 UPLOAD_DIR 指向持久化路径。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadPath).toAbsolutePath().normalize().toString()
                + File.separator;
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations("file:" + location);
    }
}
