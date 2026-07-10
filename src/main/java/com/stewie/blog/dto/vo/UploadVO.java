package com.stewie.blog.dto.vo;

import lombok.Data;

/**
 * 文件上传返回（封面图 URL）
 */
@Data
public class UploadVO {

    /** 可公开访问的图片地址，如 /uploads/xxxx.jpg */
    private String url;

    public UploadVO() {
    }

    public UploadVO(String url) {
        this.url = url;
    }
}
