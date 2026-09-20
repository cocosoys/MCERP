package com.github.cocosoys.mc.mcerp.impl;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        for (Map<String, Object> node : buildMenuTree()) {
            tree.add(toTreeselectNode(node));
        }
        return AjaxResult.success(tree);
    }

    private static TreeselectVO toTreeselectNode(Map<String, Object> node) {
        TreeselectVO vo = new TreeselectVO();
        Object id = node.get("menuId");
        vo.setId(id == null ? "" : String.valueOf(id));
        Object label = node.get("menuName");
        vo.setLabel(label == null ? "" : String.valueOf(label));
        vo.setChildren(toTreeselectChildren((List<?>) node.get("children")));
        return vo;
    }

    @SuppressWarnings("unchecked")
    private static List<TreeselectVO> toTreeselectChildren(List<?> children) {
        List<TreeselectVO> out = new ArrayList<>();
        for (Object o : children == null ? new ArrayList<>() : children) {
            out.add(toTreeselectNode((Map<String, Object>) o));
        }
        return out;
    }

    @Override
    public AjaxResult detail(String menuId) {
        ErpMenu m = DATA.get(ErpMenu.class, menuId);
        if (m != null) {
            return AjaxResult.success(m);
        }
        // 插件登记菜单为代码声明，无实体记录——返回 404 语义由前端提示
        return AjaxResult.error("仅可查看菜单表记录（插件菜单为代码声明）");
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
        m.setBuiltin("N"); // 运行时新增一律为自定义菜单
        if (m.getMenuName().isEmpty()) {
            return AjaxResult.error("菜单名称不能为空");
        }
        DATA.insert(m);
        operLog.record("菜单管理", "新增菜单", m.getMenuName(), "新增成功");
        return AjaxResult.success("新增成功");
    }

    @Override
    public AjaxResult update(String menuId, ErpMenu menu) {
        ErpMenu m = menuId == null || menuId.isEmpty() ? null : DATA.get(ErpMenu.class, menuId);
        if (m == null) {
            return AjaxResult.error("菜单不存在");
        }
        if (isBuiltinMenu(m)) {
            return AjaxResult.error("内置菜单为初始化数据，不可编辑");
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
        operLog.record("菜单管理", "修改菜单", m.getMenuName(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @Override
    public AjaxResult remove(String menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return AjaxResult.error("缺少 menuId");
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
                continue; // 内置菜单为初始化数据，跳过
            }
            DATA.deleteById(ErpMenu.class, id.trim());
            operLog.record("菜单管理", "删除菜单", m.getMenuName(), "删除成功");
        }
        return AjaxResult.success("删除成功");
    }

    private static boolean isBuiltinMenu(ErpMenu menu) {
        return menu != null && "Y".equals(menu.getBuiltin());
    }

    @Override
    public AjaxResult updateSort() {
        // 自定义菜单排序（简化：仅回执成功）
        return AjaxResult.success("操作成功");
    }

    @Override
    public AjaxResult roleMenuTreeselect(String roleId) {
        return treeselect();
    }

    // ===== 合成树 =====

    public List<Map<String, Object>> buildMenuTree() {
        List<Map<String, Object>> top = new ArrayList<>();
        // 1. 菜单表（内置初始化数据 + 自定义 erp_menu）：统一按 parentId 组装
        List<ErpMenu> menus = DATA.select(ErpMenu.class);
        menus.sort(Comparator.comparingInt(ErpMenu::getOrderNum));
        for (ErpMenu c : menus) {
            if ("0".equals(c.getParentId()) || "".equals(c.getParentId()) || c.getParentId() == null) {
                Map<String, Object> cn = sysMenuNode(c);
                cn.put("children", menuChildren(menus, c.getMenuId()));
                top.add(cn);
            }
        }
        // 2. 插件登记模块
        for (ErpModuleVO m : registry.getModules()) {
            Map<String, Object> mod = node(m.getId(), "0", m.getDisplayName(), "M", "/" + m.getId().toLowerCase(),
                    "Layout", m.getPermission(), m.getIcon() == null ? "link" : m.getIcon(), 100 + m.getSortOrder());
            mod.put("children", menuNodes(m.getChildren(), m.getId()));
            top.add(mod);
        }
        return top;
    }

    private static List<Map<String, Object>> menuChildren(List<ErpMenu> all, String parentId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ErpMenu c : all) {
            if (parentId != null && parentId.equals(c.getParentId())) {
                Map<String, Object> cn = sysMenuNode(c);
                cn.put("children", menuChildren(all, c.getMenuId()));
                out.add(cn);
            }
        }
        return out;
    }

    private static Map<String, Object> sysMenuNode(ErpMenu c) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("menuId", c.getMenuId());
        n.put("parentId", c.getParentId());
        n.put("menuName", c.getMenuName());
        n.put("orderNum", c.getOrderNum());
        n.put("path", c.getPath());
        n.put("component", c.getComponent());
        n.put("menuType", c.getMenuType());
        n.put("perms", c.getPerms());
        n.put("icon", c.getIcon());
        n.put("visible", c.getVisible());
        n.put("status", c.getStatus());
        n.put("builtin", c.getBuiltin());
        n.put("children", new ArrayList<>());
        return n;
    }

    private List<Map<String, Object>> menuNodes(List<ErpMenuVO> menus, String parentId) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (menus == null) {
            return out;
        }
        for (ErpMenuVO m : menus) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("menuId", parentId + "_" + (m.getMenuId() == null ? m.getMenuName() : m.getMenuId()));
            n.put("parentId", parentId);
            n.put("menuName", m.getMenuName());
            n.put("orderNum", m.getOrderNum());
            n.put("path", m.getPath() == null ? m.getMenuId() : m.getPath());
            n.put("component", "iframe:" + (m.getComponent() == null ? "" : m.getComponent()));
            n.put("menuType", m.getMenuType() == null ? "C" : m.getMenuType());
            n.put("perms", m.getPerms());
            n.put("icon", m.getIcon());
            n.put("visible", m.isVisible() ? "0" : "1");
            n.put("status", "0");
            n.put("children", menuNodes(m.getChildren(), (String) n.get("menuId")));
            out.add(n);
        }
        return out;
    }

    private static Map<String, Object> node(String menuId, String parentId, String menuName, String menuType,
                                            String path, String component, String perms, String icon, int order) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("menuId", menuId);
        n.put("parentId", parentId);
        n.put("menuName", menuName);
        n.put("orderNum", order);
        n.put("path", path);
        n.put("component", component);
        n.put("menuType", menuType);
        n.put("perms", perms);
        n.put("icon", icon);
        n.put("visible", "0");
        n.put("status", "0");
        n.put("children", new ArrayList<>());
        return n;
    }
}
