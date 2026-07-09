package com.stewie.blog.dto.vo;

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

    @Data
    public static class SocialLinkVO {
        private String label;
        private String href;
    }
}
