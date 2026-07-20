package com.stewie.blog.controller.admin;

import com.stewie.blog.common.Result;
import com.stewie.blog.dto.vo.DashboardStatsVO;
import com.stewie.blog.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理 API - 仪表盘统计
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.success(statsService.getDashboardStats());
    }
}
