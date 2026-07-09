package com.stewie.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stewie.blog.dto.vo.TagVO;
import com.stewie.blog.entity.Tag;
import com.stewie.blog.mapper.TagMapper;
import com.stewie.blog.service.TagService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    public List<TagVO> listTags() {
        List<Tag> tags = list(new LambdaQueryWrapper<Tag>()
                .orderByAsc(Tag::getId));
        return tags.stream().map(t -> {
            TagVO vo = new TagVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setSlug(t.getSlug());
            return vo;
        }).toList();
    }
}
