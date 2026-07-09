package com.stewie.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stewie.blog.dto.vo.AuthorVO;
import com.stewie.blog.entity.Author;

public interface AuthorService extends IService<Author> {

    /** 获取作者信息 + 社交链接 */
    AuthorVO getAuthor();
}
