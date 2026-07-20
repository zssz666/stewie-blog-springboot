package com.stewie.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stewie.blog.dto.request.AuthorUpdateRequest;
import com.stewie.blog.dto.vo.AuthorVO;
import com.stewie.blog.entity.Author;

public interface AuthorService extends IService<Author> {

    /** 获取作者信息 + 社交链接 */
    AuthorVO getAuthor();

    /** 更新作者信息 + 社交链接（后台） */
    void updateAuthor(AuthorUpdateRequest request);
}
