package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.entity.vo.DashboardStatsVO;

/**
 * 统计面板服务（抽象契约）：模块数/菜单数/附属模块列表等首页大盘数据。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.ErpDashboardServiceImpl}。
 */
public interface ErpDashboardService {

    /**
     * 计算统计面板数据：主模块数、附属模块数、目录/菜单/按钮数、模块列表。
     */
    DashboardStatsVO stats();
}
