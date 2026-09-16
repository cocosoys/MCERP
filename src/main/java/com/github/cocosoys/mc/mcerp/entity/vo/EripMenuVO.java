package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP 菜单节点（仿若依菜单模型）：
 * <ul>
 *   <li>M 目录：仅作分组，无页面，可嵌套子菜单</li>
 *   <li>C 菜单：叶子页面，url 作为 iframe 目标（RuoYi 自带 iframe 组件路径）</li>
 *   <li>F 按钮：仅权限标识，不出现在路由树</li>
 * </ul>
 */
@Data
public class EripMenuVO {

    /** 节点唯一标识（同层级内建议唯一） */
    private String id;

    /** 展示名称 */
    private String title;

    /** 图标：emoji / 图片 URL / SVG（可选） */
    private String icon;

    /** 类型：M 目录 / C 菜单 / F 按钮 */
    private String type;

    /** 路由路径（相对父级，可选，默认按 id） */
    private String path;

    /** 页面地址（type=C 时作为 iframe 目标：/plugins/&lt;插件名&gt;/... 或完整 http(s) URL） */
    private String url;

    /** 权限标识（可选，如 system:user:list） */
    private String perms;

    /** 是否可见，默认 true */
    private boolean visible = true;

    /** 排序号，小在前 */
    private int sortOrder;

    /** 子节点（仅 M 目录可有） */
    private List<EripMenuVO> children = new ArrayList<>();
}
