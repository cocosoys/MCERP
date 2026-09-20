package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.ErpMenu;
import com.github.cocosoys.mc.mcerp.service.ErpMenuService;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.DeleteMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 菜单管理（若依契约）：路由 /api/plugins/MCERP/system/menu/*。
 * 合成树（菜单表 ⊕ 插件登记菜单）与 CRUD 全部委托 {@link ErpMenuService}。
 */
@RequestMapping("/system/menu")
public class ErpMenuController {

    private final ErpMenuService menuService;

    public ErpMenuController(ErpMenuService menuService) {
        this.menuService = menuService;
    }

    @ApiName("菜单列表")
    @ApiPermission("system:menu:list")
    @GetMapping("/list")
    public AjaxResult list() {
        return menuService.list();
    }

    @ApiName("菜单树选择")
    @ApiPermission("system:menu:list")
    @GetMapping("/treeselect")
    public AjaxResult treeselect() {
        return menuService.treeselect();
    }

    @ApiName("菜单详情")
    @ApiPermission("system:menu:query")
    @GetMapping("/{menuId}")
    public AjaxResult detail(@PathVariable(name = "menuId") String menuId) {
        return menuService.detail(menuId);
    }

    @ApiName("新增菜单")
    @ApiPermission("system:menu:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody ErpMenu menu) {
        return menuService.add(menu);
    }

    @ApiName("编辑菜单")
    @ApiPermission("system:menu:edit")
    @PutMapping("/{menuId}")
    public AjaxResult update(@PathVariable(name = "menuId") String menuId, @RequestBody ErpMenu menu) {
        return menuService.update(menuId, menu);
    }

    @ApiName("删除菜单")
    @ApiPermission("system:menu:remove")
    @DeleteMapping("/{menuIds}")
    public AjaxResult remove(@PathVariable(name = "menuIds") String menuIds) {
        return menuService.remove(menuIds);
    }

    @ApiName("菜单排序")
    @ApiPermission("system:menu:edit")
    @PutMapping("/updateSort")
    public AjaxResult updateSort() {
        return menuService.updateSort();
    }

    @ApiName("角色菜单树")
    @ApiPermission("system:menu:list")
    @GetMapping("/roleMenuTreeselect/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable(name = "roleId") String roleId) {
        return menuService.roleMenuTreeselect(roleId);
    }
}
