package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP 模块描述符（字段不删减，按需求清单）：
 * <ul>
 *   <li>id：注册键，约定 = 插件名（必须唯一）</li>
 *   <li>displayName：展示名称（必填），作为主模块菜单标题</li>
 *   <li>icon：图标，emoji / 图片 / SVG 均可</li>
 *   <li>homeUrl：模块默认页地址（无子菜单时主菜单直接 iframe 打开）</li>
 *   <li>menu：主模块之下的子菜单分层（目录/菜单/按钮）</li>
 *   <li>sortOrder：排序号，小在前</li>
 *   <li>permission：访问该模块所需的权限标识（可选，空则不校验）</li>
 * </ul>
 */
@Data
public class EripModuleVO {

    /** 注册键 = 插件名 */
    private String id;

    /** 展示名称（必填） */
    private String displayName;

    /** 图标（emoji / 图片 / SVG） */
    private String icon;

    /** 默认页地址 */
    private String homeUrl;

    /** 子菜单分层 */
    private List<EripMenuVO> children = new ArrayList<>();

    /** 排序号 */
    private int sortOrder;

    /** 访问权限标识（可选） */
    private String permission;
}
