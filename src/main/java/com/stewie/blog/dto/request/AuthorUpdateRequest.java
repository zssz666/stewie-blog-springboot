package com.stewie.blog.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 作者信息更新请求（后台）
 */
@Data
public class AuthorUpdateRequest {

    private String name;
    private String role;
    private String bio;
    private String avatar;
    private List<String> skills;
    private List<SocialLinkInput> socials;

    @Data
    public static class SocialLinkInput {
        private String label;
        private String href;
    }
}
