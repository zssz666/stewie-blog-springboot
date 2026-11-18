package com.stewie.blog.dto.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 作者视图对象（对齐前端 Author 接口）
 */
@Data
public class AuthorVO {

    private Long id;
    private String name;
    private String role;
    private String bio;
    private String avatar;
    private List<String> skills;
    private List<SocialLinkVO> socials;
    private List<MovieCover> movies;

    public record MovieCover(
            @NotBlank(message = "请填写影视标题") String title,
            @NotBlank(message = "请填写封面链接或上传图片") String img) {
    }

    @Data
    public static class SocialLinkVO {
        private String label;
        private String href;
    }
}
