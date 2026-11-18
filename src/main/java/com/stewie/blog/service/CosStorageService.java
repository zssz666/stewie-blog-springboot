package com.stewie.blog.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 腾讯云 COS 对象存储封装。
 * <p>上传成功后返回公有读基础地址拼接的对象 URL（如
 * https://stewie-blog-1412052351.cos.ap-chengdu.myqcloud.com/covers/uuid.jpg），
 * 前端 resolveAsset 已支持完整 https URL，无需额外处理。</p>
 */
@Service
public class CosStorageService {

    private static final Logger log = LoggerFactory.getLogger(CosStorageService.class);

    private final COSClient cosClient;
    private final String secretId;
    private final String bucket;
    private final String baseUrl;
    private final String prefix;

    public CosStorageService(COSClient cosClient,
                             @Value("${cos.secret-id:}") String secretId,
                             @Value("${cos.bucket:}") String bucket,
                             @Value("${cos.base-url:}") String baseUrl,
                             @Value("${cos.prefix:covers}") String prefix) {
        this.cosClient = cosClient;
        this.secretId = secretId;
        this.bucket = bucket;
        String normalized = (baseUrl == null) ? "" : baseUrl.trim();
        this.baseUrl = normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        this.prefix = (prefix == null) ? "" : prefix.trim();
    }

    /** 是否已正确配置 COS 凭证（缺凭证时由调用方降级到本地存储） */
    public boolean isConfigured() {
        return secretId != null && !secretId.isBlank()
                && bucket != null && !bucket.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }

    /** 上传文件到 COS（使用配置的默认目录前缀，如 covers），返回公有读访问 URL */
    public String upload(MultipartFile file) {
        return upload(file, prefix);
    }

    /**
     * 上传文件到 COS 指定目录，返回公有读访问 URL。
     * <p>用于把不同用途的文件分目录存放（如 images/ 正文图、videos/ 正文视频、covers/ 封面），
     * 但仍在同一个 bucket 内。folder 为空时退回配置的默认前缀。</p>
     */
    public String upload(MultipartFile file, String folder) {
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "jpg";
        String useFolder = (folder == null || folder.isBlank()) ? prefix : folder.trim();
        String safePrefix = (useFolder == null || useFolder.isBlank()) ? "" : useFolder + "/";
        String key = safePrefix + UUID.randomUUID() + "." + ext;

        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        // 必须给出可播放/可渲染的 Content-Type：若客户端发的是 application/octet-stream
        // （curl、部分浏览器、.mov/.m4v 常见），COS 会照存，导致 <video> 拒绝播放、图片不渲染。
        meta.setContentType(resolveContentType(file.getContentType(), ext));

        try {
            PutObjectRequest req = new PutObjectRequest(bucket, key, file.getInputStream(), meta);
            cosClient.putObject(req);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "读取上传文件失败");
        } catch (Exception e) {
            log.error("COS 上传失败 bucket={} key={}", bucket, key, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "上传到 COS 失败：" + e.getMessage());
        }
        return baseUrl + "/" + key;
    }

    /** 扩展名 → MIME 兜底表（客户端 Content-Type 不可信时使用） */
    private static final Map<String, String> EXT_MIME = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("m4v", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("webm", "video/webm"),
            Map.entry("ogv", "video/ogg"));

    /**
     * 决定写入 COS 的 Content-Type。
     * <p>客户端的 Content-Type 只有在「明确是 image/* 或 video/*」时才采信；
     * 其余情况（null、application/octet-stream、binary/octet-stream 等）一律按扩展名兜底，
     * 否则浏览器拿到 octet-stream 的视频会直接不播。</p>
     */
    private static String resolveContentType(String declared, String ext) {
        if (declared != null) {
            String ct = declared.trim().toLowerCase();
            if (ct.startsWith("image/") || ct.startsWith("video/")) {
                return ct;
            }
        }
        String byExt = EXT_MIME.get(ext);
        return byExt != null ? byExt : "application/octet-stream";
    }
}
