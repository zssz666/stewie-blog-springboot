package com.stewie.blog.controller.admin;

import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.Result;
import com.stewie.blog.common.ResultCode;
import com.stewie.blog.dto.vo.UploadVO;
import com.stewie.blog.service.CosStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 管理 API - 封面图上传（需 JWT）
 * <p>优先上传到腾讯云 COS（公有读，返回可直接访问的 https URL）；
 * 当 COS 未配置凭证时降级到本地磁盘，保留对旧本地封面图的兼容。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final CosStorageService cosStorage;
    private final String uploadPath;
    private final String urlPrefix;

    public UploadController(CosStorageService cosStorage,
                            @Value("${upload.path:./uploads}") String uploadPath,
                            @Value("${upload.url-prefix:/uploads}") String urlPrefix) {
        this.cosStorage = cosStorage;
        this.uploadPath = uploadPath;
        this.urlPrefix = urlPrefix;
    }

    @PostMapping("/upload")
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        validate(file);
        String url;
        if (cosStorage.isConfigured()) {
            url = cosStorage.upload(file);
        } else {
            // 降级：COS 未配置时写本地磁盘（保留对旧本地封面图的兼容）
            log.warn("COS 未配置，封面上传降级到本地磁盘 {}", uploadPath);
            url = saveLocal(file);
        }
        return Result.success(new UploadVO(url));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持上传图片文件");
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "jpg";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的图片格式");
        }
    }

    private String saveLocal(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "jpg";
        String filename = UUID.randomUUID() + "." + ext;
        Path dirPath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path dest = dirPath.resolve(filename);
        try {
            Files.createDirectories(dirPath);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件保存失败");
        }
        return urlPrefix + "/" + filename;
    }
}
