package com.stewie.blog.service;

import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.ResultCode;
import com.stewie.blog.service.CosStorageService.GalleryAlbum;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GalleryAccessService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int BCRYPT_MAX_BYTES = 72;
    private static final int MAX_ATTEMPTS = 8;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(5);
    private static final Duration LINK_LIFETIME = Duration.ofMinutes(15);

    private final CosStorageService storage;
    private final JdbcTemplate database;
    private final PasswordEncoder encoder;
    private final Map<AttemptKey, AttemptWindow> attempts = new HashMap<>();

    public GalleryAccessService(CosStorageService storage, JdbcTemplate database, PasswordEncoder encoder) {
        this.storage = storage;
        this.database = database;
        this.encoder = encoder;
    }

    public List<GalleryAlbum> listPublicAlbums() {
        var albums = storage.listGalleryAlbums();
        var access = albumAccess();
        Date expiresAt = Date.from(Instant.now().plus(LINK_LIFETIME));
        return albums.stream().map(album -> {
            if (Boolean.TRUE.equals(access.get(album.name()))) {
                return new GalleryAlbum(album.name(), null, List.of(), true);
            }
            return access.containsKey(album.name()) ? storage.signGalleryAlbum(album, expiresAt) : album;
        }).toList();
    }

    public List<GalleryAlbum> listAdminAlbums() {
        var access = albumAccess();
        Date expiresAt = Date.from(Instant.now().plus(LINK_LIFETIME));
        return storage.listGalleryAlbums().stream().map(album -> {
            var signed = storage.signGalleryAlbum(album, expiresAt);
            return new GalleryAlbum(signed.name(), signed.cover(), signed.photos(), Boolean.TRUE.equals(access.get(album.name())));
        }).toList();
    }

    private Map<String, Boolean> albumAccess() {
        return database.query("SELECT album_name, password_hash IS NOT NULL AS locked FROM t_gallery_album_access", rows -> {
            Map<String, Boolean> result = new HashMap<>();
            while (rows.next()) result.put(rows.getString("album_name"), rows.getBoolean("locked"));
            return result;
        });
    }

    public void setPassword(String album, String password) {
        validatePassword(password);
        var gallery = storage.getGalleryAlbum(album);
        // COS 权限可能只更新了一部分，先让公开页面也能通过签名链接继续显示。
        registerAlbumForSignedLinks(album);
        storage.makeGalleryAlbumPrivate(gallery);
        String hash = encoder.encode(password);
        database.update("UPDATE t_gallery_album_access SET password_hash = ? WHERE album_name = ?", hash, album);
    }

    public String uploadPhoto(MultipartFile file, String album) {
        CosStorageService.requireAlbumName(album);
        registerAlbumForSignedLinks(album);
        return storage.uploadGalleryPhoto(file, album, Date.from(Instant.now().plus(LINK_LIFETIME)));
    }

    public void setLiveVideo(MultipartFile file, String album, String name) {
        CosStorageService.requireAlbumName(album);
        registerAlbumForSignedLinks(album);
        storage.setGalleryLiveVideo(file, album, name);
    }

    private void registerAlbumForSignedLinks(String album) {
        database.update("""
                INSERT INTO t_gallery_album_access (album_name) VALUES (?)
                ON DUPLICATE KEY UPDATE album_name = album_name
                """, album);
    }

    public void removePassword(String album) {
        // 保留记录，让已经设为私有的照片继续使用签名链接展示。
        database.update("UPDATE t_gallery_album_access SET password_hash = NULL WHERE album_name = ?", album);
    }

    public record UnlockedAlbum(GalleryAlbum album, long expiresAt) {}

    public UnlockedAlbum unlock(String album, String password, String address) {
        validatePassword(password);
        var hashes = database.query("SELECT password_hash FROM t_gallery_album_access WHERE album_name = ?",
                (row, index) -> row.getString(1), album);
        if (hashes.isEmpty() || hashes.get(0) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "这个相册没有启用查看密码，请刷新页面");
        }

        var key = new AttemptKey(album, address);
        countAttempt(key);
        if (!encoder.matches(password, hashes.get(0))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "相册密码不正确");
        }
        clearAttempts(key);

        var gallery = storage.getGalleryAlbum(album);
        var expiresAt = Instant.now().plus(LINK_LIFETIME);
        return new UnlockedAlbum(storage.signGalleryAlbum(gallery, Date.from(expiresAt)), expiresAt.toEpochMilli());
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > BCRYPT_MAX_BYTES || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码至少 8 个字符，最多 72 字节");
        }
    }

    private record AttemptKey(String album, String address) {}

    private record AttemptWindow(Instant expiresAt, int count) {}

    private synchronized void countAttempt(AttemptKey key) {
        Instant now = Instant.now();
        attempts.values().removeIf(window -> !window.expiresAt().isAfter(now));
        var window = attempts.getOrDefault(key, new AttemptWindow(now.plus(ATTEMPT_WINDOW), 0));
        if (window.count() >= MAX_ATTEMPTS) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "尝试次数过多，请过几分钟再试");
        }
        attempts.put(key, new AttemptWindow(window.expiresAt(), window.count() + 1));
    }

    private synchronized void clearAttempts(AttemptKey key) {
        attempts.remove(key);
    }
}
