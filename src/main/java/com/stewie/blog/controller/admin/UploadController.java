package com.stewie.blog.controller.admin;

import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.Result;
import com.stewie.blog.common.ResultCode;
import com.stewie.blog.dto.vo.UploadVO;
import com.stewie.blog.service.CosStorageService;
import com.stewie.blog.service.CosStorageService.GalleryAlbum;
import com.stewie.blog.service.GalleryAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 管理 API - 媒体上传（需 JWT）
 *
 * <p>同时服务于「封面图」与「正文插图 / 插视频」：按文件类型自动分流校验与分目录存放。</p>
 * <ul>
 *   <li>图片：{@code image/*}，默认 ≤5MB，进 COS 的 {@code images/} 目录（封面图走 {@code covers/}）</li>
 *   <li>视频：{@code video/*}，默认 ≤100MB，进 COS 的 {@code videos/} 目录</li>
 * </ul>
 * <p>优先上传到腾讯云 COS（公有读，返回可直接访问的 https URL）；COS 未配置凭证时降级到本地磁盘，
 * 保留对旧本地封面图的兼容。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final String KIND_IMAGE = "image";
    private static final String KIND_VIDEO = "video";
    private static final long GALLERY_PHOTO_MAX_SIZE = 10L * 1024 * 1024;

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "webm", "mov", "m4v", "ogv");

    /** 封面图沿用历史目录 covers/，正文媒体分到 images/ 与 videos/ */
    private static final String COVER_FOLDER = "covers";
    private static final String IMAGE_FOLDER = "images";
    private static final String VIDEO_FOLDER = "videos";

    private final CosStorageService cosStorage;
    private final GalleryAccessService galleryAccess;
    private final String uploadPath;
    private final String urlPrefix;
    private final long imageMaxSize;
    private final long videoMaxSize;

    public UploadController(CosStorageService cosStorage,
                            GalleryAccessService galleryAccess,
                            @Value("${upload.path:./uploads}") String uploadPath,
                            @Value("${upload.url-prefix:/uploads}") String urlPrefix,
                            @Value("${upload.image-max-size:5242880}") long imageMaxSize,
                            @Value("${upload.video-max-size:104857600}") long videoMaxSize) {
        this.cosStorage = cosStorage;
        this.galleryAccess = galleryAccess;
        this.uploadPath = uploadPath;
        this.urlPrefix = urlPrefix;
        this.imageMaxSize = imageMaxSize;
        this.videoMaxSize = videoMaxSize;
    }

    /**
     * 上传媒体文件。
     *
     * @param file  文件本体
     * @param type  可选，显式指定 {@code image} / {@code video}；不传则按 Content-Type 与扩展名自动判断
     * @param scene 可选，{@code cover} 表示封面图（进 covers/ 目录）；默认按正文媒体处理
     */
    @PostMapping("/upload")
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "type", required = false) String type,
                                   @RequestParam(value = "scene", required = false) String scene) {
        requireFile(file);
        String kind = resolveKind(file, type);
        String ext = extensionOf(file);
        String folder;
        long maxSize;

        if (KIND_VIDEO.equals(kind)) {
            if (!VIDEO_EXT.contains(ext)) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "不支持的视频格式：" + ext + "（支持 mp4 / webm / mov / m4v / ogv）");
            }
            maxSize = videoMaxSize;
            folder = VIDEO_FOLDER;
        } else {
            requireImageExtension(ext);
            maxSize = imageMaxSize;
            folder = "cover".equalsIgnoreCase(scene) ? COVER_FOLDER : IMAGE_FOLDER;
        }

        validateMedia(file, kind, maxSize);

        String url;
        if (cosStorage.isConfigured()) {
            url = cosStorage.upload(file, folder);
        } else {
            log.warn("COS 未配置，媒体上传降级到本地磁盘 {}/{}", uploadPath, folder);
            url = saveLocal(file, folder, ext);
        }
        log.info("媒体上传成功 type={} size={} url={}", kind, file.getSize(), url);
        return Result.success(new UploadVO(url, kind, file.getSize()));
    }

    @GetMapping("/photos")
    public ResponseEntity<Result<List<GalleryAlbum>>> photos() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.success(galleryAccess.listAdminAlbums()));
    }

    @PutMapping("/photos/albums/password")
    public Result<Void> setAlbumPassword(@RequestParam("album") String album, @RequestBody PasswordInput input) {
        galleryAccess.setPassword(album, input.password());
        return Result.success();
    }

    @DeleteMapping("/photos/albums/password")
    public Result<Void> removeAlbumPassword(@RequestParam("album") String album) {
        galleryAccess.removePassword(album);
        return Result.success();
    }

    public record PasswordInput(String password) {}

    @PostMapping("/photos")
    public Result<UploadVO> uploadPhoto(@RequestParam("file") MultipartFile file, @RequestParam("album") String album) {
        requireFile(file);
        requireImageExtension(extensionOf(file));
        validateMedia(file, KIND_IMAGE, GALLERY_PHOTO_MAX_SIZE);
        String url = galleryAccess.uploadPhoto(file, album);
        return Result.success(new UploadVO(url, KIND_IMAGE, file.getSize()));
    }

    @PostMapping("/photos/albums")
    public Result<Void> createAlbum(@RequestParam("album") String album) {
        cosStorage.createGalleryAlbum(album);
        return Result.success();
    }

    @PutMapping("/photos/albums/cover")
    public Result<Void> setAlbumCover(@RequestParam("album") String album, @RequestParam("name") String name) {
        cosStorage.setGalleryCover(album, name);
        return Result.success();
    }

    @DeleteMapping("/photos")
    public Result<Void> deletePhoto(@RequestParam("album") String album, @RequestParam("name") String name) {
        cosStorage.deleteGalleryPhoto(album, name);
        return Result.success();
    }

    private static void requireFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的文件");
        }
    }

    private static void requireImageExtension(String ext) {
        if (!IMAGE_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "不支持的图片格式：" + ext + "（支持 jpg / png / gif / webp）");
        }
    }

    private void validateMedia(MultipartFile file, String kind, long maxSize) {
        String label = KIND_VIDEO.equals(kind) ? "视频" : "图片";
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "大小不能超过 " + humanSize(maxSize)
                    + "（当前 " + humanSize(file.getSize()) + "）");
        }
        // 扩展名和 Content-Type 都由客户端提供，不能单独用来判断文件类型。
        if (!matchesMagic(file, kind)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件内容与格式不符，请确认上传的是真实的" + label + "文件");
        }
    }

    /** 判断媒体类型：显式 type 参数优先，其次 Content-Type，最后按扩展名兜底 */
    private String resolveKind(MultipartFile file, String type) {
        if (type != null) {
            String t = type.trim().toLowerCase(Locale.ROOT);
            if (KIND_VIDEO.equals(t)) {
                return KIND_VIDEO;
            }
            if (KIND_IMAGE.equals(t)) {
                return KIND_IMAGE;
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.startsWith("video/")) {
                return KIND_VIDEO;
            }
            if (ct.startsWith("image/")) {
                return KIND_IMAGE;
            }
        }
        return VIDEO_EXT.contains(extensionOf(file)) ? KIND_VIDEO : KIND_IMAGE;
    }

    private String extensionOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            int dot = original.lastIndexOf('.');
            if (dot < original.length() - 1) {
                return original.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }
        return "";
    }

    /**
     * 嗅探文件头（magic number）判断真实类型，与声明的 kind 是否一致。
     * <p>覆盖白名单内全部格式：jpg / png / gif / webp、mp4·mov·m4v（ftyp）、webm·mkv、ogv。</p>
     */
    private boolean matchesMagic(MultipartFile file, String kind) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "读取上传文件失败");
        }
        if (read < 12) {
            // 过小的文件不可能是有效的图片 / 视频
            return false;
        }
        String real = sniff(head);
        return real != null && real.equals(kind);
    }

    private String sniff(byte[] h) {
        int b0 = h[0] & 0xFF, b1 = h[1] & 0xFF, b2 = h[2] & 0xFF, b3 = h[3] & 0xFF;
        // JPEG
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) {
            return KIND_IMAGE;
        }
        // PNG
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) {
            return KIND_IMAGE;
        }
        // GIF
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) {
            return KIND_IMAGE;
        }
        // WEBP：前 4 字节 "RIFF"，第 9-12 字节 "WEBP"
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
                && (h[8] & 0xFF) == 0x57 && (h[9] & 0xFF) == 0x45
                && (h[10] & 0xFF) == 0x42 && (h[11] & 0xFF) == 0x50) {
            return KIND_IMAGE;
        }
        // MP4 / MOV / M4V：第 5-8 字节为 "ftyp"
        if (h[4] == 0x66 && h[5] == 0x74 && h[6] == 0x79 && h[7] == 0x70) {
            return KIND_VIDEO;
        }
        // WebM / MKV：EBML 头
        if (b0 == 0x1A && b1 == 0x45 && b2 == 0xDF && b3 == 0xA3) {
            return KIND_VIDEO;
        }
        // Ogg（ogv）："OggS"
        if (b0 == 0x4F && b1 == 0x67 && b2 == 0x67 && b3 == 0x53) {
            return KIND_VIDEO;
        }
        return null;
    }

    private String saveLocal(MultipartFile file, String folder, String ext) {
        String safeExt = ext.isEmpty() ? "bin" : ext;
        String filename = UUID.randomUUID() + "." + safeExt;
        Path dirPath = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(folder);
        Path dest = dirPath.resolve(filename);
        try {
            Files.createDirectories(dirPath);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件保存失败");
        }
        return urlPrefix + "/" + folder + "/" + filename;
    }

    /** 人类可读的字节大小，用于错误提示 */
    private static String humanSize(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return (bytes / (1024L * 1024L)) + "MB";
        }
        if (bytes >= 1024L) {
            return (bytes / 1024L) + "KB";
        }
        return bytes + "B";
    }
}
