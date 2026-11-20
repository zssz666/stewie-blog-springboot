package com.stewie.blog.service;

import com.stewie.blog.common.BusinessException;
import com.stewie.blog.common.ResultCode;
import com.stewie.blog.service.CosStorageService.GalleryAlbum;
import com.stewie.blog.service.CosStorageService.GalleryPhoto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GalleryAccessServiceTest {

    private static final String ALBUM = "Family";
    private static final String TEST_PASSWORD = "fixture-passphrase";
    private static final String ADDRESS = "192.0.2.10";

    private final CosStorageService storage = mock(CosStorageService.class);
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final GalleryAccessService access = new GalleryAccessService(storage, db, encoder);
    private final GalleryPhoto photo = new GalleryPhoto("photo.jpg", "https://test.example/my-imgs/Family/photo.jpg",
            "https://test.example/my-imgs/Family/photo.jpg.live");
    private final GalleryAlbum album = new GalleryAlbum(ALBUM, photo, List.of(photo));

    @Test
    void publicListingHidesBothCoverAndPhotosOfLockedAlbums() {
        var open = new GalleryAlbum("Public", null, List.of());
        when(storage.listGalleryAlbums()).thenReturn(List.of(album, open));
        when(db.query(anyString(), ArgumentMatchers.<ResultSetExtractor<Map<String, Boolean>>>any()))
                .thenReturn(Map.of(ALBUM, true));

        assertThat(access.listPublicAlbums()).containsExactly(
                new GalleryAlbum(ALBUM, null, List.of(), true), open);
        verify(storage, never()).signGalleryAlbum(eq(album), any(Date.class));
    }

    @Test
    void validatesPasswordBeforeIssuingTemporaryLinks() {
        when(db.query(anyString(), ArgumentMatchers.<RowMapper<String>>any(), eq(ALBUM)))
                .thenReturn(List.of(encoder.encode(TEST_PASSWORD)));
        when(storage.getGalleryAlbum(ALBUM)).thenReturn(album);
        var signedPhoto = new GalleryPhoto("photo.jpg", "https://test.example/photo.jpg?signature=fixture");
        var signedAlbum = new GalleryAlbum(ALBUM, signedPhoto, List.of(signedPhoto));
        when(storage.signGalleryAlbum(eq(album), any(Date.class))).thenReturn(signedAlbum);

        assertThatThrownBy(() -> access.unlock(ALBUM, "wrong-password", ADDRESS))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode()));
        verify(storage, never()).signGalleryAlbum(eq(album), any(Date.class));

        var unlocked = access.unlock(ALBUM, TEST_PASSWORD, ADDRESS);
        assertThat(unlocked.album()).isEqualTo(signedAlbum);
        assertThat(unlocked.expiresAt()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void leavesAlbumBrowsableIfCosCannotApplyPrivacy() {
        when(storage.getGalleryAlbum(ALBUM)).thenReturn(album);
        doThrow(new BusinessException(ResultCode.FORBIDDEN, "COS ACL denied"))
                .when(storage).makeGalleryAlbumPrivate(album);

        assertThatThrownBy(() -> access.setPassword(ALBUM, TEST_PASSWORD)).isInstanceOf(BusinessException.class);
        verify(db).update(startsWith("INSERT INTO t_gallery_album_access (album_name)"), eq(ALBUM));
        verify(db, never()).update(startsWith("UPDATE t_gallery_album_access SET password_hash"), any(), any());
    }

    @Test
    void appliesCosPrivacyBeforeActivatingPassword() {
        when(storage.getGalleryAlbum(ALBUM)).thenReturn(album);

        access.setPassword(ALBUM, TEST_PASSWORD);

        var order = inOrder(db, storage);
        order.verify(db).update(startsWith("INSERT INTO t_gallery_album_access (album_name)"), eq(ALBUM));
        order.verify(storage).makeGalleryAlbumPrivate(album);
        var hash = ArgumentCaptor.forClass(String.class);
        order.verify(db).update(startsWith("UPDATE t_gallery_album_access SET password_hash"), hash.capture(), eq(ALBUM));
        assertThat(encoder.matches(TEST_PASSWORD, hash.getValue())).isTrue();
    }
}
