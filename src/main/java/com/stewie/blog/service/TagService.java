package com.stewie.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stewie.blog.dto.vo.TagVO;
import com.stewie.blog.entity.Tag;

import java.util.List;

public interface TagService extends IService<Tag> {

    /** 标签列表 */
    List<TagVO> listTags();
}
