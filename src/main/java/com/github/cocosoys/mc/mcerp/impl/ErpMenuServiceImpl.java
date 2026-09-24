package com.github.cocosoys.mc.mcerp.impl;

import static com.github.cocosoys.mc.mcerp.i18n.McerpI18n.t;

import com.github.cocosoys.mc.mcerp.ErpRegistry;
import com.github.cocosoys.mc.mcerp.entity.ErpMenu;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpMenuVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpModuleVO;
import com.github.cocosoys.mc.mcerp.entity.vo.TreeselectVO;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.ErpMenuService;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 菜单管理实现（从 ErpMenuController 迁入）：
 * 合成树（菜单表 ⊕ 插件登记菜单）+ CRUD；内置菜单（builtin='Y'）为初始化数据不可改删。
 */
public class ErpMenuServiceImpl implements ErpMenuService {

    private final ErpRegistry registry;
    private final OperLogService operLog;

    public ErpMenuServiceImpl(ErpRegistry registry, OperLogService operLog) {
        this.registry = registry;
        this.operLog = operLog;
    }

    @Override
    public AjaxResult list() {
        return AjaxResult.success(buildMenuTree());
    }

    @Override
    public AjaxResult treeselect() {
        List<TreeselectVO> tree = new ArrayList<>();
        for (ErpMenuVO node : buildMenuTree()) {
            tree.add(toTreeselectNode(node));
        }
        return AjaxResult.success(tree);
    }

    private static TreeselectVO toTreeselectNode(ErpMenuVO node) {
        TreeselectVO vo = new TreeselectVO();
        vo.setId(node.getMenuId() == null ? "" : node.getMenuId());
        vo.setLabel(node.getMenuName() == null ? "" : node.getMenuName());
        vo.setChildren(toTreeselectChildren(node.getChildren()));
        return vo;
    }

    private static List<TreeselectVO> toTreeselectChildren(List<ErpMenuVO> children) {
        List<TreeselectVO> out = new ArrayList<>();
        if (children == null) {
            return out;
        }
        for (ErpMenuVO c : children) {
            out.add(toTreeselectNode(c));
        }
        return out;
    }

    @Override
    public AjaxResult detail(String menuId) {
        ErpMenu m = DATA.get(ErpMenu.class, menuId);
        if (m != null) {
            return AjaxResult.success(m);
        }
        return AjaxResult.error(t("mcerp.menu.table-only", "仅可查看菜单表记录（插件菜单为代码声明）"));
    }

    @Override
    public AjaxResult add(ErpMenu menu) {
        ErpMenu m = new ErpMenu();
        m.setMenuId(UUID.randomUUID().toString());
        m.setParentId(menu.getParentId() == null || menu.getParentId().isEmpty() ? "0" : menu.getParentId());
        m.setMenuName(menu.getMenuName() == null ? "" : menu.getMenuName());
        m.setOrderNum(menu.getOrderNum());
        m.setPath(menu.getPath() == null ? "" : menu.getPath());
        m.setComponent(menu.getComponent() == null ? "" : menu.getComponent());
        m.setMenuType(menu.getMenuType() == null || menu.getMenuType().isEmpty() ? "C" : menu.getMenuType());
        m.setPerms(menu.getPerms() == null ? "" : menu.getPerms());
        m.setIcon(menu.getIcon() == null ? "" : menu.getIcon());
        m.setVisible(menu.getVisible() == null || menu.getVisible().isEmpty() ? "0" : menu.getVisible());
        m.setStatus(menu.getStatus() == null || menu.getStatus().isEmpty() ? "0" : menu.getStatus());
        m.setBuiltin("N");
        if (m.getMenuName().isEmpty()) {
            return AjaxResult.error(t("mcerp.menu.name-empty", "菜单名称不能为空"));
        }
        DATA.insert(m);
        operLog.record(t("mcerp.operlog.module.menu", "菜单管理"), t("mcerp.operlog.action.add-menu", "新增菜单"), m.getMenuName(), t("mcerp.common.add-success", "新增成功"));
        return AjaxResult.success(t("mcerp.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(String menuId, ErpMenu menu) {
        ErpMenu m = menuId == null || menuId.isEmpty() ? null : DATA.get(ErpMenu.class, menuId);
        if (m == null) {
            return AjaxResult.error(t("mcerp.menu.not-found", "菜单不存在"));
        }
        if (isBuiltinMenu(m)) {
            return AjaxResult.error(t("mcerp.menu.builtin-readonly", "内置菜单为初始化数据，不可编辑"));
        }
        if (menu.getParentId() != null) {
            m.setParentId(menu.getParentId());
        }
        if (menu.getMenuName() != null) {
            m.setMenuName(menu.getMenuName());
        }
        m.setOrderNum(menu.getOrderNum());
        if (menu.getPath() != null) {
            m.setPath(menu.getPath());
        }
        if (menu.getComponent() != null) {
            m.setComponent(menu.getComponent());
        }
        if (menu.getMenuType() != null) {
            m.setMenuType(menu.getMenuType());
        }
        if (menu.getPerms() != null) {
            m.setPerms(menu.getPerms());
        }
        if (menu.getIcon() != null) {
            m.setIcon(menu.getIcon());
        }
        if (menu.getVisible() != null) {
            m.setVisible(menu.getVisible());
        }
        if (menu.getStatus() != null) {
            m.setStatus(menu.getStatus());
        }
        DATA.updateById(m);
        operLog.record(t("mcerp.operlog.module.menu", "菜单管理"), t("mcerp.operlog.action.edit-menu", "修改菜单"), m.getMenuName(), t("mcerp.common.edit-success", "修改成功"));
        return AjaxResult.success(t("mcerp.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return AjaxResult.error(t("mcerp.menu.missing-id", "缺少 menuId"));
        }
        for (String id : menuIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            ErpMenu m = DATA.get(ErpMenu.class, id.trim());
            if (m == null) {
                continue;
            }
            if (isBuiltinMenu(m)) {
                continue;
            }
            DATA.deleteById(ErpMenu.class, id.trim());
            operLog.record(t("mcerp.operlog.module.menu", "菜单管理"), t("mcerp.operlog.action.delete-menu", "删除菜单"), m.getMenuName(), t("mcerp.common.delete-success", "删除成功"));
        }
        return AjaxResult.success(t("mcerp.common.delete-success", "删除成功"));
    }

    private static boolean isBuiltinMenu(ErpMenu menu) {
        return menu != null && "Y".equals(menu.getBuiltin());
    }

    @Override
    public AjaxResult updateSort() {
        return AjaxResult.success(t("mcerp.common.operation-success", "操作成功"));
    }

    @Override
    public AjaxResult roleMenuTreeselect(String roleId) {
        return treeselect();
    }

    // ===== 合成树 =====

    public List<ErpMenuVO> buildMenuTree() {
        List<ErpMenuVO> top = new ArrayList<>();
        List<ErpMenu> menus = DATA.select(ErpMenu.class);
        menus.sort(Comparator.comparingInt(ErpMenu::getOrderNum));
        for (ErpMenu c : menus) {
            if ("0".equals(c.getParentId()) || "".equals(c.getParentId()) || c.getParentId() == null) {
                ErpMenuVO cn = toMenuVO(c);
                cn.setChildren(menuChildren(menus, c.getMenuId()));
                top.add(cn);
            }
        }
        for (ErpModuleVO m : registry.getModules()) {
            ErpMenuVO mod = new ErpMenuVO();
            mod.setMenuId(m.getId());
            mod.setParentId("0");
            mod.setMenuName(m.getDisplayName());
            mod.setOrderNum(100 + m.getSortOrder());
            mod.setPath("/" + m.getId().toLowerCase());
            mod.setComponent("Layout");
            mod.setMenuType("M");
            mod.setPerms(m.getPermission());
            mod.setIcon(m.getIcon() == null ? "link" : m.getIcon());
            mod.setVisible("0");
            mod.setStatus("0");
            mod.setChildren(menuNodes(m.getChildren(), m.getId(), m.getComponentMode()));
            top.add(mod);
        }
        return top;
    }

    private static List<ErpMenuVO> menuChildren(List<ErpMenu> all, String parentId) {
        List<ErpMenuVO> out = new ArrayList<>();
        for (ErpMenu c : all) {
            if (parentId != null && parentId.equals(c.getParentId())) {
                ErpMenuVO cn = toMenuVO(c);
                cn.setChildren(menuChildren(all, c.getMenuId()));
                out.add(cn);
            }
        }
        return out;
    }

    private static ErpMenuVO toMenuVO(ErpMenu c) {
        ErpMenuVO vo = new ErpMenuVO();
        vo.setMenuId(c.getMenuId());
        vo.setParentId(c.getParentId());
        vo.setMenuName(c.getMenuName());
        vo.setOrderNum(c.getOrderNum());
        vo.setPath(c.getPath());
        vo.setComponent(c.getComponent());
        vo.setMenuType(c.getMenuType());
        vo.setPerms(c.getPerms());
        vo.setIcon(c.getIcon());
        vo.setVisible(c.getVisible());
        vo.setStatus(c.getStatus());
        vo.setBuiltin(c.getBuiltin());
        return vo;
    }

    private List<ErpMenuVO> menuNodes(List<ErpMenuVO> menus, String parentId, String mode) {
        List<ErpMenuVO> out = new ArrayList<>();
        if (menus == null) {
            return out;
        }
        for (ErpMenuVO m : menus) {
            ErpMenuVO n = new ErpMenuVO();
            String childId = m.getMenuId() == null ? m.getMenuName() : m.getMenuId();
            n.setMenuId(parentId + "_" + childId);
            n.setParentId(parentId);
            n.setMenuName(m.getMenuName());
            n.setOrderNum(m.getOrderNum());
            n.setPath(m.getPath() == null ? m.getMenuId() : m.getPath());
            String prefix = "WUJIE".equals(mode) ? "wujie:" : "iframe:";
            n.setComponent(prefix + (m.getComponent() == null ? "" : m.getComponent()));
            n.setMenuType(m.getMenuType() == null ? "C" : m.getMenuType());
            n.setPerms(m.getPerms());
            n.setIcon(m.getIcon());
            n.setVisible(m.isVisible() ? "0" : "1");
            n.setStatus("0");
            n.setChildren(menuNodes(m.getChildren(), n.getMenuId(), mode));
            out.add(n);
        }
        return out;
    }
}
