package com.stewie.blog.controller.pub;

import com.stewie.blog.common.Result;
import com.stewie.blog.service.CosStorageService;
import com.stewie.blog.service.CosStorageService.GalleryAlbum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PublicPhotoController {

    private final CosStorageService storage;

    public PublicPhotoController(CosStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/api/photos")
    public Result<List<GalleryAlbum>> photos() {
        return Result.success(storage.listGalleryAlbums());
    }
}
