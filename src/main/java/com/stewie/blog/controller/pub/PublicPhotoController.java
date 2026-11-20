package com.stewie.blog.controller.pub;

import com.stewie.blog.common.Result;
import com.stewie.blog.service.CosStorageService.GalleryAlbum;
import com.stewie.blog.service.GalleryAccessService;
import com.stewie.blog.service.GalleryAccessService.UnlockedAlbum;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PublicPhotoController {

    private final GalleryAccessService access;

    public PublicPhotoController(GalleryAccessService access) {
        this.access = access;
    }

    @GetMapping("/api/photos")
    public ResponseEntity<Result<List<GalleryAlbum>>> photos() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.success(access.listPublicAlbums()));
    }

    @PostMapping("/api/photos/unlock")
    public ResponseEntity<Result<UnlockedAlbum>> unlock(@RequestBody UnlockInput input, HttpServletRequest request) {
        var album = access.unlock(input.album(), input.password(), request.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.success(album));
    }

    public record UnlockInput(String album, String password) {}
}
