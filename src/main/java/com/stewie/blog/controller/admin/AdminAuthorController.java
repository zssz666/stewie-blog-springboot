package com.stewie.blog.controller.admin;

import com.stewie.blog.common.Result;
import com.stewie.blog.dto.request.AuthorUpdateRequest;
import com.stewie.blog.service.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理 API - 作者信息更新
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthorController {

    private final AuthorService authorService;

    @PutMapping("/author")
    public Result<Void> updateAuthor(@Valid @RequestBody AuthorUpdateRequest req) {
        authorService.updateAuthor(req);
        return Result.success();
    }
}
