package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.entity.SysMenu;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 菜单管理服务（抽象契约）：菜单表（内置 ⊕ 自定义）⊕ 插件登记菜单的合成树、CRUD。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.SysMenuServiceImpl}；
 * 内置菜单（builtin='Y'）不可改删等一致性编排由 impl 负责。
 */
public interface SysMenuService {

    AjaxResult list();

    AjaxResult treeselect();

    AjaxResult detail(String menuId);

    AjaxResult add(SysMenu menu);

    AjaxResult update(String menuId, SysMenu menu);

    AjaxResult remove(String menuIds);

    AjaxResult updateSort();

    AjaxResult roleMenuTreeselect(String roleId);
}
