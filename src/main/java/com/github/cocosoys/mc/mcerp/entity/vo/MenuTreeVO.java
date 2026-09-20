package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单权限树节点 VO（权限分配弹窗的 el-tree 数据：menuId/parentId/menuName/perms/menuType/children）。
 */
@Data
public class MenuTreeVO {

    /** 菜单 ID（表 erp_menu.menu_id；插件声明菜单为合成键 parentId_声明id） */
    private String menuId;

    /** 父菜单 ID（顶层为 '0'） */
    private String parentId;

    /** 菜单名称（展示名） */
    private String menuName;

    /** 权限标识（F 按钮节点的权限字符串，如 soys.erp.user.add） */
    private String perms;

    /** 菜单类型：M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    /** 子节点（递归菜单树） */
    private List<MenuTreeVO> children = new ArrayList<>();

}
