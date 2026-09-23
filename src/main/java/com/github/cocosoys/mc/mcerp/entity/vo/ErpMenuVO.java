package com.github.cocosoys.mc.mcerp.entity.vo;

import com.github.cocosoys.mc.mcerp.entity.ErpMenu;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP 菜单节点 VO。直接继承 {@link ErpMenu}——全部业务属性即菜单表字段
 * （menuId/menuName/parentId/orderNum/path/component/query/routeName/isFrame/isCache/
 * menuType/visible/status/perms/icon/builtin 及审计字段），无自有业务字段；
 * 仅扩展菜单树结构（{@link #children}）与可见性便捷判断（{@link #isVisible()}）。
 *
 * <ul>
 *   <li>M 目录（menuType='M'）：仅作分组，无页面，可嵌套子菜单</li>
 *   <li>C 菜单（menuType='C'）：叶子页面，component 作为 iframe 目标（RuoYi 自带 iframe 组件路径）</li>
 *   <li>F 按钮（menuType='F'）：仅权限标识，不出现在路由树</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
/**
 * 菜单视图对象：继承 ErpMenu，含 children 树与插件声明字段。
 */
public class ErpMenuVO extends ErpMenu {

    /** 子节点（仅 M 目录可有；由 DSL 层级管理，MenuSpec 不提供） */
    private List<ErpMenuVO> children = new ArrayList<>();

    /** 可见性便捷判断：表字段 '0' 显示 / '1' 隐藏；null（未设置）视为显示。 */
    public boolean isVisible() {
        return getVisible() == null || "0".equals(getVisible());
    }
}
