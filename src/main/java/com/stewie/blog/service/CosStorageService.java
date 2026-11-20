package com.stewie.blog.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
    private static final String GALLERY_PREFIX = "my-imgs/";
    private static final String UNCATEGORIZED_ALBUM = "未归档";
    private static final String ALBUM_COVER = ".album-cover";
    private static final int MAX_ALBUM_NAME_LENGTH = 64;
    private static final int MAX_COS_KEY_BYTES = 850;
    private static final Duration COS_PROBE_TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient ANONYMOUS_COS = HttpClient.newBuilder().connectTimeout(COS_PROBE_TIMEOUT).build();

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
        String ext = uploadExtension(file);
        String useFolder = (folder == null || folder.isBlank()) ? prefix : folder.trim();
        String safePrefix = (useFolder == null || useFolder.isBlank()) ? "" : useFolder + "/";
        String key = safePrefix + UUID.randomUUID() + "." + ext;
        return put(file, key, resolveContentType(file.getContentType(), ext));
    }

    public record GalleryPhoto(String name, String url) {}

    public record GalleryAlbum(String name, GalleryPhoto cover, List<GalleryPhoto> photos, boolean locked) {
        public GalleryAlbum(String name, GalleryPhoto cover, List<GalleryPhoto> photos) {
            this(name, cover, photos, false);
        }
    }

    public List<GalleryAlbum> listGalleryAlbums() {
        requireGalleryStorage();
        Map<String, List<COSObjectSummary>> albums = new TreeMap<>(Comparator.reverseOrder());
        for (COSObjectSummary object : listGalleryObjects(GALLERY_PREFIX)) {
            if (!object.getKey().startsWith(GALLERY_PREFIX)) continue;
            String relativeKey = object.getKey().substring(GALLERY_PREFIX.length());
            int folderSeparator = relativeKey.indexOf('/');
            String album = folderSeparator < 0 ? UNCATEGORIZED_ALBUM : relativeKey.substring(0, folderSeparator);
            if (folderSeparator >= 0 && (!isAlbumName(album) || UNCATEGORIZED_ALBUM.equals(album))) continue;

            String file = relativeKey.substring(folderSeparator + 1);
            boolean albumMarker = ALBUM_COVER.equals(file) || (folderSeparator >= 0 && file.isEmpty());
            if (albumMarker || isGalleryImage(file)) {
                albums.computeIfAbsent(album, ignored -> new ArrayList<>()).add(object);
            }
        }
        return albums.entrySet().stream()
                .sorted(Comparator.comparing(album -> UNCATEGORIZED_ALBUM.equals(album.getKey())))
                .map(album -> galleryAlbum(album.getKey(), album.getValue()))
                .toList();
    }

    public GalleryAlbum getGalleryAlbum(String name) {
        if (UNCATEGORIZED_ALBUM.equals(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请先把照片放进独立相册，再设置查看密码");
        }
        requireGalleryStorage();
        String folder = albumPrefix(name);
        var objects = listGalleryObjects(folder);
        if (objects.isEmpty()) throw new BusinessException(ResultCode.NOT_FOUND, "没有找到这个相册");
        return galleryAlbum(name, objects);
    }

    public GalleryAlbum signGalleryAlbum(GalleryAlbum album, Date expiresAt) {
        String folder = albumPrefix(album.name());
        var photos = album.photos().stream()
                .map(photo -> new GalleryPhoto(photo.name(), signGalleryPhoto(folder + photo.name(), expiresAt)))
                .toList();
        var cover = photos.stream().filter(photo -> album.cover() != null && photo.name().equals(album.cover().name()))
                .findFirst().orElse(null);
        return new GalleryAlbum(album.name(), cover, photos, album.locked());
    }

    private String signGalleryPhoto(String key, Date expiresAt) {
        var request = new GeneratePresignedUrlRequest(bucket, key, HttpMethodName.GET);
        request.setExpiration(expiresAt);
        return cosClient.generatePresignedUrl(request).toExternalForm();
    }

    public void makeGalleryAlbumPrivate(GalleryAlbum album) {
        String folder = albumPrefix(album.name());
        for (var photo : album.photos()) {
            String key = folder + photo.name();
            makeGalleryObjectPrivate(key);
            requirePrivateGalleryObject(key, photo.url());
        }
    }

    private void makeGalleryObjectPrivate(String key) {
        try {
            cosClient.setObjectAcl(bucket, key, CannedAccessControlList.Private);
        } catch (CosServiceException exception) {
            if (exception.getStatusCode() == 403) {
                throw new BusinessException(ResultCode.FORBIDDEN, "后端 COS 密钥无法修改相册权限，请授权 cos:PutObjectACL");
            }
            throw exception;
        }
    }

    private void requirePrivateGalleryObject(String key, String configuredUrl) {
        String origin = cosClient.getObjectUrl(bucket, key).toExternalForm();
        requireAnonymousReadDenied(origin);
        if (!origin.equals(configuredUrl)) requireAnonymousReadDenied(configuredUrl);
    }

    private static void requireAnonymousReadDenied(String url) {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(COS_PROBE_TIMEOUT)
                .header("Cache-Control", "no-cache").GET().build();
        try {
            var response = ANONYMOUS_COS.send(request, HttpResponse.BodyHandlers.ofInputStream());
            response.body().close();
            int status = response.statusCode();
            if (status != 403) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "未能确认 COS 直链已禁止匿名访问，请检查存储桶的公开访问策略（HTTP " + status + "）");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "COS 权限检查中断，请重试");
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "无法检查 COS 权限，请稍后重试");
        }
    }

    private List<COSObjectSummary> listGalleryObjects(String folder) {
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(folder);
        var objects = new ArrayList<COSObjectSummary>();
        ObjectListing page;
        do {
            page = cosClient.listObjects(request);
            objects.addAll(page.getObjectSummaries());
            request.setMarker(page.getNextMarker());
        } while (page.isTruncated());

        return objects;
    }

    private GalleryAlbum galleryAlbum(String album, List<COSObjectSummary> objects) {
        String folder = albumPrefix(album);
        String coverKey = folder + ALBUM_COVER;
        List<GalleryPhoto> photos = objects.stream()
                .filter(object -> isGalleryImage(object.getKey().substring(folder.length())))
                .sorted(Comparator.comparing(COSObjectSummary::getLastModified).reversed())
                .map(object -> {
                    String name = object.getKey().substring(folder.length());
                    return new GalleryPhoto(name, baseUrl + "/" + UriUtils.encodePath(folder, StandardCharsets.UTF_8)
                            + UriUtils.encode(name, StandardCharsets.UTF_8));
                })
                .toList();
        if (photos.isEmpty()) return new GalleryAlbum(album, null, photos);

        // Without a designated cover, use the most recently added photo in this album.
        GalleryPhoto cover = photos.get(0);
        if (objects.stream().anyMatch(object -> coverKey.equals(object.getKey()))) {
            String selectedName = readCoverName(coverKey);
            cover = photos.stream().filter(photo -> photo.name().equals(selectedName)).findFirst().orElse(cover);
        }
        return new GalleryAlbum(album, cover, photos);
    }

    private String readCoverName(String coverKey) {
        try (var cover = cosClient.getObject(bucket, coverKey)) {
            byte[] content = cover.getObjectContent().readNBytes(MAX_COS_KEY_BYTES + 1);
            if (content.length > MAX_COS_KEY_BYTES) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "相册封面配置超出 COS 对象名长度");
            }
            return new String(content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log.error("读取相册封面配置失败 key={}", coverKey, exception);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "读取相册封面失败");
        }
    }

    public void createGalleryAlbum(String album) {
        if (UNCATEGORIZED_ALBUM.equals(album)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "「未归档」是保留的相册名");
        }
        requireGalleryStorage();
        String folder = albumPrefix(album);
        if (!cosClient.doesObjectExist(bucket, folder)) {
            putAlbumMetadata(folder, "");
        }
    }

    public void setGalleryCover(String album, String name) {
        String folder = albumPrefix(album);
        requireImageName(name);
        requireGalleryStorage();
        if (!cosClient.doesObjectExist(bucket, folder + name)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "相册中找不到这张照片");
        }
        putAlbumMetadata(folder + ALBUM_COVER, name);
    }

    String uploadGalleryPhoto(MultipartFile file, String album, Date expiresAt) {
        String folder = albumPrefix(album);
        requireGalleryStorage();
        String ext = uploadExtension(file);
        String key = folder + UUID.randomUUID() + "." + ext;
        put(file, key, EXT_MIME.get(ext));
        return signGalleryPhoto(key, expiresAt);
    }

    public void deleteGalleryPhoto(String album, String name) {
        String folder = albumPrefix(album);
        requireImageName(name);
        requireGalleryStorage();
        cosClient.deleteObject(bucket, folder + name);
    }

    private void putAlbumMetadata(String key, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        var metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        metadata.setContentType("text/plain; charset=utf-8");
        cosClient.putObject(new PutObjectRequest(bucket, key, new ByteArrayInputStream(bytes), metadata));
    }

    private static String albumPrefix(String name) {
        requireAlbumName(name);
        return UNCATEGORIZED_ALBUM.equals(name) ? GALLERY_PREFIX : GALLERY_PREFIX + name + "/";
    }

    static void requireAlbumName(String name) {
        if (!isAlbumName(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "相册名无效，请使用不含斜杠的名称");
        }
    }

    private static boolean isAlbumName(String name) {
        return name != null && !name.isBlank() && name.equals(name.strip()) && name.length() <= MAX_ALBUM_NAME_LENGTH
                && !name.equals(".") && !name.equals("..")
                && name.chars().noneMatch(character -> character == '/' || character == '\\' || Character.isISOControl(character));
    }

    private static void requireImageName(String name) {
        if (!isGalleryImage(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "照片文件名无效");
        }
    }

    private void requireGalleryStorage() {
        if (!isConfigured()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "照片墙的 COS 存储未配置");
        }
    }

    private static boolean isGalleryImage(String name) {
        int extensionStart = name.lastIndexOf('.');
        if (extensionStart <= 0 || name.chars().anyMatch(character -> character == '/' || character == '\\'
                || Character.isISOControl(character))) return false;
        String extension = name.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
        return EXT_MIME.getOrDefault(extension, "").startsWith("image/");
    }

    private static String uploadExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        return (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "jpg";
    }

    private String put(MultipartFile file, String key, String contentType) {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        // 必须给出可播放/可渲染的 Content-Type：若客户端发的是 application/octet-stream
        // （curl、部分浏览器、.mov/.m4v 常见），COS 会照存，导致 <video> 拒绝播放、图片不渲染。
        meta.setContentType(contentType);

        try {
            PutObjectRequest req = new PutObjectRequest(bucket, key, file.getInputStream(), meta);
            if (key.startsWith(GALLERY_PREFIX)) {
                req.withCannedAcl(CannedAccessControlList.Private);
            }
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
