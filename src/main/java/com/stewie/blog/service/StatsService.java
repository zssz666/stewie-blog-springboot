package com.stewie.blog.service;

import com.stewie.blog.dto.vo.DashboardStatsVO;

public interface StatsService {

    /** 管理后台仪表盘统计 */
    DashboardStatsVO getDashboardStats();
}
