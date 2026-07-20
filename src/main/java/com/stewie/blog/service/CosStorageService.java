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

    /** 上传文件到 COS，返回公有读访问 URL */
    public String upload(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "jpg";
        String safePrefix = (prefix == null || prefix.isBlank()) ? "" : prefix + "/";
        String key = safePrefix + UUID.randomUUID() + "." + ext;

        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        meta.setContentType(file.getContentType());

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
}
