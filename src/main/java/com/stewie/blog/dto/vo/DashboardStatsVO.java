package com.stewie.blog.dto.vo;

import lombok.Data;

/**
 * 管理后台仪表盘统计数据
 */
@Data
public class DashboardStatsVO {
    private long postsTotal;
    private long postsPublished;
    private long commentsTotal;
    private long commentsPending;
    private long viewsTotal;
    private long likesTotal;
}
