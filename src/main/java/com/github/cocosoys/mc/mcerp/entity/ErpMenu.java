package com.github.cocosoys.mc.mcerp.entity;

import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * ERP 菜单表（若依菜单模型：目录 M / 菜单 C / 按钮 F）。
 * 内置菜单为 resources 初始化数据（data/erp_menu.yml 与 sql/mcerp-init.sql，builtin='Y'）；
 * 插件注册菜单经 getRouters 合成，不落库。
 * 审计字段（createTime/updateTime 等）继承 {@link BaseEntity}。
 */
@TableName("erp_menu")
@Data
/**
 * 菜单实体（erp_menu 表）。
 */
public class ErpMenu extends BaseEntity {

    /** 菜单 ID（主键，自然数或 UUID） */
    @TableId
    private String menuId;

    /** 菜单名称 */
    private String menuName;

    /** 父菜单 ID（顶层为 '0'） */
    private String parentId;

    /** 显示顺序（小在前） */
    private int orderNum;

    /** 路由地址（相对父级；外链时为完整 URL） */
    private String path;

    /** 组件路径（如 system/user/index；M 目录为 Layout/ParentView） */
    private String component;

    /** 路由参数（如 id=1，非空时拼接到路由 path 后） */
    private String query;

    /** 路由名称（前端路由 name；为空时后端按 path 首字母大写生成） */
    private String routeName;

    /** 是否外链（0 是 / 1 否，与若依一致；'0' 时前端经 meta.link 新窗口打开） */
    private String isFrame;

    /** 是否缓存（0 缓存 / 1 不缓存；不缓存时前端 keep-alive 排除） */
    private String isCache;

    /** 菜单类型：M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    /** 显示状态（0 显示 / 1 隐藏） */
    private String visible;

    /** 菜单状态（0 正常 / 1 停用） */
    private String status;

    /** 权限标识（如 system:user:list） */
    private String perms;

    /** 菜单图标（emoji / 图标名 / URL） */
    private String icon;

    /** 是否内置初始化数据（Y 内置 / N 自定义；控制器据此禁止修改删除内置行） */
    private String builtin;
}
