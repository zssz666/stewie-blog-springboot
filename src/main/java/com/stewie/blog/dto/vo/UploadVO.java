package com.stewie.blog.dto.vo;

import lombok.Data;

/**
 * 文件上传返回（封面图 / 正文图片 / 正文视频的公开访问 URL）
 */
@Data
public class UploadVO {

    /** 可公开访问的地址，如 https://xxx.cos.ap-chengdu.myqcloud.com/images/uuid.jpg */
    private String url;

    /** 媒体类型：image / video（供前端决定插入哪种标签） */
    private String type;

    /** 文件字节数 */
    private Long size;

    public UploadVO() {
    }

    public UploadVO(String url) {
        this.url = url;
    }

    public UploadVO(String url, String type, Long size) {
        this.url = url;
        this.type = type;
        this.size = size;
    }
}
