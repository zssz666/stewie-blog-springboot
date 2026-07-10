package com.stewie.blog.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞状态视图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeVO {

    /** 当前指纹是否已点赞 */
    private boolean liked;

    /** 文章当前总点赞数 */
    private long likes;
}
