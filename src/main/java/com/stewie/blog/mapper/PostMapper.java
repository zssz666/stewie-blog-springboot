package com.stewie.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stewie.blog.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 全文搜索：ngram 解析器，按相关性倒序。
     * 索引列必须与 MATCH 列完全一致（title, excerpt, content）。
     */
    @Select("SELECT * FROM t_post WHERE deleted = 0 AND status = 1 " +
            "AND MATCH(title, excerpt, content) AGAINST (#{q} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY MATCH(title, excerpt, content) AGAINST (#{q} IN NATURAL LANGUAGE MODE) DESC")
    Page<Post> search(Page<Post> page, @Param("q") String q);

    @Select("SELECT COALESCE(SUM(views), 0) FROM t_post WHERE deleted = 0")
    long sumViews();

    @Select("SELECT COALESCE(SUM(likes), 0) FROM t_post WHERE deleted = 0")
    long sumLikes();
}
