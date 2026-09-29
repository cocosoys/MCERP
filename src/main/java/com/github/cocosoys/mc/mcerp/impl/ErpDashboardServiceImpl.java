package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.ErpRegistry;
import com.github.cocosoys.mc.mcerp.entity.vo.DashboardStatsVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpMenuVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpModuleVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ModuleItemVO;
import com.github.cocosoys.mc.mcerp.service.ErpDashboardService;

import java.util.List;

/**
 * 统计面板服务实现：统计已登记模块、菜单树 M/C/F 数量，组装 DashboardStatsVO。
 */
public class ErpDashboardServiceImpl implements ErpDashboardService {

    private final ErpRegistry registry;

    public ErpDashboardServiceImpl(ErpRegistry registry) {
        this.registry = registry;
    }

    @Override
    public DashboardStatsVO stats() {
        List<ErpModuleVO> modules = registry.getModules();
        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setMainModules(1);
        stats.setTotalModules(modules.size());

        for (ErpModuleVO m : modules) {
            boolean isMain = "MCERP".equalsIgnoreCase(m.getId());
            if (!isMain) {
                stats.setAddonModules(stats.getAddonModules() + 1);
            }
            int[] counts = countMenus(m.getChildren());
            stats.setDirs(stats.getDirs() + counts[0]);
            stats.setMenus(stats.getMenus() + counts[1]);
            stats.setPerms(stats.getPerms() + counts[2]);

            ModuleItemVO item = new ModuleItemVO();
            item.setId(m.getId());
            item.setName(m.getDisplayName());
            item.setType(isMain ? "main" : "addon");
            item.setMenus(counts[1]);
            stats.getModuleList().add(item);
        }
        return stats;
    }

    /** 递归统计菜单树中的 M/C/F 数量。返回 [dirs, menus, perms]。 */
    private int[] countMenus(List<ErpMenuVO> children) {
        int[] r = new int[3];
        if (children == null) {
            return r;
        }
        for (ErpMenuVO node : children) {
            if (node.getMenuType() == null) {
                continue;
            }
            switch (node.getMenuType()) {
                case "M":
                    r[0]++;
                    break;
                case "C":
                    r[1]++;
                    break;
                case "F":
                    r[2]++;
                    break;
                default:
                    break;
            }
            int[] sub = countMenus(node.getChildren());
            r[0] += sub[0];
            r[1] += sub[1];
            r[2] += sub[2];
        }
        return r;
    }
}
