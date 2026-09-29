package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP 菜单节点 VO（菜单管理树 / 路由树 / 权限树合成时的通用节点模型）。
 *
 * <p>独立于表实体 {@code ErpMenu}（其主键 menuId/parentId 为 Long 自增数字）：
 * 本 VO 的 menuId/parentId 保持 String 语义，统一承载两类节点——</p>
 * <ul>
 *   <li>表菜单节点：menuId = 表主键的字符串形式（如 "1"），parentId = 父主键字符串（根 "0"）；</li>
 *   <li>插件声明节点：menuId = 声明标识（如 "user"）或合成键（父链 + "_" + 声明标识，
 *       如 "soyshttpovermcerp_config"），parentId = 父合成键或 "0"。</li>
 * </ul>
 * 字段名与 erp_menu 表完全一致（无自有业务字段），仅扩展菜单树结构（{@link #children}）
 * 与可见性便捷判断（{@link #isVisible()}）。
 *
 * <ul>
 *   <li>M 目录（menuType='M'）：仅作分组，无页面，可嵌套子菜单</li>
 *   <li>C 菜单（menuType='C'）：叶子页面，component 作为 iframe/wujie 目标</li>
 *   <li>F 按钮（menuType='F'）：仅权限标识，不出现在路由树</li>
 * </ul>
 */
@Data
/**
 * 菜单视图对象：菜单树节点（含 children），menuId/parentId 为 String 语义。
 */
public class ErpMenuVO {

    /** 菜单 ID（表主键字符串形式 / 插件声明标识 / 合成键） */
    private String menuId;

    /** 菜单名称（展示名） */
    private String menuName;

    /** 父菜单 ID（顶层为 "0"） */
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

    /** 子节点（仅 M 目录可有；由 DSL 层级管理，MenuSpec 不提供） */
    private List<ErpMenuVO> children = new ArrayList<>();

    /** 可见性便捷判断：表字段 '0' 显示 / '1' 隐藏；null（未设置）视为显示。 */
    public boolean isVisible() {
        return getVisible() == null || "0".equals(getVisible());
    }
}
