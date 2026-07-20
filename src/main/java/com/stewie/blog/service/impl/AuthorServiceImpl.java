package com.stewie.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stewie.blog.dto.request.AuthorUpdateRequest;
import com.stewie.blog.dto.vo.AuthorVO;
import com.stewie.blog.entity.Author;
import com.stewie.blog.entity.SocialLink;
import com.stewie.blog.mapper.AuthorMapper;
import com.stewie.blog.mapper.SocialLinkMapper;
import com.stewie.blog.service.AuthorService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AuthorServiceImpl extends ServiceImpl<AuthorMapper, Author> implements AuthorService {

    private final SocialLinkMapper socialLinkMapper;
    private final ObjectMapper objectMapper;

    public AuthorServiceImpl(SocialLinkMapper socialLinkMapper, ObjectMapper objectMapper) {
        this.socialLinkMapper = socialLinkMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public AuthorVO getAuthor() {
        Author author = getOne(new LambdaQueryWrapper<Author>().last("LIMIT 1"));
        if (author == null) {
            return null;
        }
        AuthorVO vo = new AuthorVO();
        vo.setId(author.getId());
        vo.setName(author.getName());
        vo.setRole(author.getRole());
        vo.setBio(author.getBio());
        vo.setAvatar(author.getAvatar());
        vo.setSkills(parseSkills(author.getSkills()));

        List<SocialLink> links = socialLinkMapper.selectList(
                new LambdaQueryWrapper<SocialLink>()
                        .eq(SocialLink::getAuthorId, author.getId())
                        .orderByAsc(SocialLink::getSort));
        vo.setSocials(links.stream().map(l -> {
            AuthorVO.SocialLinkVO sl = new AuthorVO.SocialLinkVO();
            sl.setLabel(l.getLabel());
            sl.setHref(l.getHref());
            return sl;
        }).toList());

        return vo;
    }

    @Override
    public void updateAuthor(AuthorUpdateRequest req) {
        Author author = getOne(new LambdaQueryWrapper<Author>().last("LIMIT 1"));
        if (author == null) {
            author = new Author();
        }
        if (req.getName() != null) author.setName(req.getName());
        if (req.getRole() != null) author.setRole(req.getRole());
        if (req.getBio() != null) author.setBio(req.getBio());
        if (req.getAvatar() != null) author.setAvatar(req.getAvatar());
        if (req.getSkills() != null) {
            try {
                author.setSkills(objectMapper.writeValueAsString(req.getSkills()));
            } catch (Exception ignored) {
                // 序列化失败则保留原值
            }
        }
        saveOrUpdate(author);

        if (req.getSocials() != null) {
            socialLinkMapper.delete(new LambdaQueryWrapper<SocialLink>()
                    .eq(SocialLink::getAuthorId, author.getId()));
            int sort = 0;
            for (AuthorUpdateRequest.SocialLinkInput s : req.getSocials()) {
                if (s == null || s.getLabel() == null || s.getLabel().isBlank()) {
                    continue;
                }
                SocialLink sl = new SocialLink();
                sl.setAuthorId(author.getId());
                sl.setLabel(s.getLabel());
                sl.setHref(s.getHref() == null ? "" : s.getHref());
                sl.setSort(sort++);
                socialLinkMapper.insert(sl);
            }
        }
    }

    private List<String> parseSkills(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
