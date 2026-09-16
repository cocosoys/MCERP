package com.github.cocosoys.mc.mcerp.entity;

import lombok.Data;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;

/**
 * 系统菜单表（若依菜单模型：目录 M / 菜单 C / 按钮 F）。
 * 内置菜单为 resources 初始化数据（data/erp_menu.yml 与 sql/mcerp-init.sql，builtin='Y'）；
 * 插件注册菜单经 getRouters 合成，不落库。
 */
@TableName("erp_menu")
@Data
public class SysMenu {

    @TableId
    private String menuId;

    private String parentId;

    private String menuName;

    private int orderNum;

    private String path;

    private String component;

    /** M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    private String perms;

    private String icon;

    private String visible;

    private String status;

    /** Y 内置初始化数据 / N 自定义（控制器据此禁止修改删除内置行） */
    private String builtin;
}
