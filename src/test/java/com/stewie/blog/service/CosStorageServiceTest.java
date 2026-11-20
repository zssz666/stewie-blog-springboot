package com.stewie.blog.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.PutObjectRequest;
import com.stewie.blog.common.BusinessException;
import com.stewie.blog.service.CosStorageService.GalleryAlbum;
import com.stewie.blog.service.CosStorageService.GalleryPhoto;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CosStorageServiceTest {

    private static final String BUCKET = "gallery-test-1";
    private static final String BASE_URL = "https://gallery.example.com";

    private final COSClient client = mock(COSClient.class);
    private final CosStorageService storage = new CosStorageService(client, "test-id", BUCKET, BASE_URL, "covers");

    @Test
    void listsAlbumsAcrossPagesWithTheChosenCoverAndPreservesUnfiledPhotos() {
        ObjectListing first = new ObjectListing();
        first.getObjectSummaries().add(summary("my-imgs/2026-11/old.jpg", 10));
        first.getObjectSummaries().add(summary("my-imgs/2026-11/.album-cover", 20));
        first.getObjectSummaries().add(summary("my-imgs/随拍/", 20));
        first.getObjectSummaries().add(summary("my-imgs/unfiled.jpg", 25));
        first.getObjectSummaries().add(summary("my-imgs/", 20));
        first.getObjectSummaries().add(summary("my-imgs/2026-11/private/hidden.jpg", 20));
        first.setTruncated(true);
        first.setNextMarker("page-two");

        ObjectListing second = new ObjectListing();
        second.getObjectSummaries().add(summary("my-imgs/2026-11/晚霞 +#1.PNG", 30));
        second.getObjectSummaries().add(summary("my-imgs/notes.txt", 40));
        second.getObjectSummaries().add(summary("covers/unrelated.jpg", 40));

        when(client.listObjects(any(ListObjectsRequest.class))).thenAnswer(invocation -> {
            ListObjectsRequest request = invocation.getArgument(0);
            assertThat(request.getBucketName()).isEqualTo(BUCKET);
            assertThat(request.getPrefix()).isEqualTo("my-imgs/");
            assertThat(request.getDelimiter()).isNull();
            return switch (String.valueOf(request.getMarker())) {
                case "null" -> first;
                case "page-two" -> second;
                default -> throw new AssertionError("Unexpected marker: " + request.getMarker());
            };
        });

        var coverObject = new COSObject();
        var coverBytes = new ByteArrayInputStream("old.jpg".getBytes(StandardCharsets.UTF_8));
        coverObject.setObjectContent(new COSObjectInputStream(coverBytes, new HttpGet()));
        when(client.getObject(BUCKET, "my-imgs/2026-11/.album-cover")).thenReturn(coverObject);
        var newest = new GalleryPhoto("晚霞 +#1.PNG", BASE_URL + "/my-imgs/2026-11/%E6%99%9A%E9%9C%9E%20%2B%231.PNG");
        var cover = new GalleryPhoto("old.jpg", BASE_URL + "/my-imgs/2026-11/old.jpg");
        var unfiled = new GalleryPhoto("unfiled.jpg", BASE_URL + "/my-imgs/unfiled.jpg");

        assertThat(storage.listGalleryAlbums()).containsExactlyInAnyOrder(
                new GalleryAlbum("2026-11", cover, List.of(newest, cover)),
                new GalleryAlbum("随拍", null, List.of()),
                new GalleryAlbum("未归档", unfiled, List.of(unfiled)));
    }

    @Test
    void signsLiveVideosAlongsideTheirPhotosWithoutAddingExtraTiles() {
        var listing = new ObjectListing();
        String photoKey = "my-imgs/Family/photo.jpg";
        String liveKey = photoKey + ".live";
        listing.getObjectSummaries().add(summary(photoKey, 10));
        listing.getObjectSummaries().add(summary(liveKey, 20));
        when(client.listObjects(any(ListObjectsRequest.class))).thenReturn(listing);
        when(client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenAnswer(invocation -> {
            var request = invocation.getArgument(0, GeneratePresignedUrlRequest.class);
            return URI.create(BASE_URL + "/" + request.getKey() + "?signature=fixture").toURL();
        });

        Date expiresAt = new Date(1000);
        var album = storage.signGalleryAlbum(storage.listGalleryAlbums().get(0), expiresAt);

        assertThat(album.photos()).singleElement().satisfies(photo ->
                assertThat(photo.liveVideoUrl()).isEqualTo(BASE_URL + "/" + liveKey + "?signature=fixture"));
        var signedRequests = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
        verify(client, times(2)).generatePresignedUrl(signedRequests.capture());
        assertThat(signedRequests.getAllValues()).extracting(GeneratePresignedUrlRequest::getKey)
                .containsExactlyInAnyOrder(photoKey, liveKey);
    }

    @Test
    void deletionRejectsOtherPathsAndTargetsOnlyTheGallery() {
        assertThatThrownBy(() -> storage.deleteGalleryPhoto("../covers", "cover.jpg"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> storage.deleteGalleryPhoto("2026-11", "private\\hidden.jpg"))
                .isInstanceOf(BusinessException.class);
        verify(client, never()).deleteObject(any(String.class), any(String.class));

        storage.deleteGalleryPhoto("2026-11", "晚霞 +#1.PNG");

        verify(client).deleteObject(BUCKET, "my-imgs/2026-11/晚霞 +#1.PNG");
        verify(client).deleteObject(BUCKET, "my-imgs/2026-11/晚霞 +#1.PNG.live");
    }

    @Test
    void galleryUploadStoresPrivatePhotosAndReturnsASignedLink() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.PNG", "image/svg+xml", new byte[] { 1 });
        Date expiresAt = new Date(1000);
        var signedUrl = URI.create(BASE_URL + "/my-imgs/photo.png?signature=fixture").toURL();
        when(client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(signedUrl);

        String url = storage.uploadGalleryPhoto(file, "2026-11", expiresAt);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture());
        assertThat(request.getValue().getKey()).startsWith("my-imgs/2026-11/").endsWith(".png");
        assertThat(request.getValue().getMetadata().getContentType()).isEqualTo("image/png");
        assertThat(request.getValue().getCannedAcl()).isEqualTo(CannedAccessControlList.Private);
        assertThat(url).isEqualTo(signedUrl.toExternalForm());
    }

    @Test
    void coverCanOnlyPointToAnImageInTheSameAlbum() {
        assertThatThrownBy(() -> storage.setGalleryCover("2026-11", "missing.jpg"))
                .isInstanceOf(BusinessException.class);

        when(client.doesObjectExist(BUCKET, "my-imgs/2026-11/cover.jpg")).thenReturn(true);
        storage.setGalleryCover("2026-11", "cover.jpg");

        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture());
        assertThat(request.getValue().getKey()).isEqualTo("my-imgs/2026-11/.album-cover");
    }

    @Test
    void protectsOldPhotosEvenIfTheFolderMarkerIsPublic() throws Exception {
        String folder = "my-imgs/Family/";
        String key = folder + "photo.jpg";
        Set<String> privateKeys = ConcurrentHashMap.newKeySet();
        HttpServer origin = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin.createContext("/", exchange -> {
            String objectKey = exchange.getRequestURI().getPath().substring(1);
            exchange.sendResponseHeaders(privateKeys.contains(objectKey) ? 403 : 200, -1);
            exchange.close();
        });
        origin.start();
        try {
            String url = "http://127.0.0.1:" + origin.getAddress().getPort();
            var galleryStorage = new CosStorageService(client, "test-id", BUCKET, url, "covers");
            var photo = new GalleryPhoto("photo.jpg", url + "/" + key);
            when(client.getObjectUrl(eq(BUCKET), anyString())).thenAnswer(invocation ->
                    URI.create(url + "/" + invocation.getArgument(1, String.class)).toURL());
            doAnswer(invocation -> {
                privateKeys.add(key);
                return null;
            }).when(client).setObjectAcl(BUCKET, key, CannedAccessControlList.Private);

            galleryStorage.makeGalleryAlbumPrivate(new GalleryAlbum("Family", photo, List.of(photo)));

            assertThat(privateKeys).containsExactly(key);
        } finally {
            origin.stop(0);
        }
    }

    private static COSObjectSummary summary(String key, long lastModified) {
        COSObjectSummary summary = new COSObjectSummary();
        summary.setKey(key);
        summary.setLastModified(new Date(lastModified));
        return summary;
    }
}
