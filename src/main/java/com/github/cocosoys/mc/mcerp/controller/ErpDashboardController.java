package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.service.ErpDashboardService;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPublic;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 统计面板控制器：首页大盘数据。
 * 路由 /api/plugins/MCERP/dashboard/*。
 * 仅做请求路由与参数透传，业务逻辑在 {@link ErpDashboardService} 实现。
 */
@RequestMapping("/dashboard")
public class ErpDashboardController {

    private final ErpDashboardService dashboardService;

    public ErpDashboardController(ErpDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** 首页统计数据（已登录即可访问）。 */
    @ApiPublic
    @ApiName("统计面板")
    @GetMapping("/stats")
    public AjaxResult stats() {
        return AjaxResult.success(dashboardService.stats());
    }
}
