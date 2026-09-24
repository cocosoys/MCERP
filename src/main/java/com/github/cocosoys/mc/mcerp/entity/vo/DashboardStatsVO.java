package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计面板数据 VO。
 */
@Data
public class DashboardStatsVO {

    /** 主模块数量 */
    private int mainModules;

    /** 附属模块数量 */
    private int addonModules;

    /** 总模块数量 */
    private int totalModules;

    /** 目录数（M） */
    private int dirs;

    /** 菜单数（C） */
    private int menus;

    /** 按钮数（F） */
    private int perms;

    /** 模块列表 */
    private List<ModuleItemVO> moduleList = new ArrayList<>();
}
