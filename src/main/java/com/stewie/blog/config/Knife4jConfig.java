package com.stewie.blog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 3 文档配置
 * 访问地址：http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI stewieBlogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stewie Blog API")
                        .description("Stewie Blog 后端接口文档 - Spring Boot 3 + MyBatis-Plus")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Stewie")
                                .url("https://github.com/stewie")
                                .email("stewie@example.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
