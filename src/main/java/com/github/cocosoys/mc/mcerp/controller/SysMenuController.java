package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.EripMenu;
import com.github.cocosoys.mc.mcerp.EripModule;
import com.github.cocosoys.mc.mcerp.EripRegistry;
import com.github.cocosoys.mc.mcerp.entity.SysMenu;
import com.github.cocosoys.mc.mcerp.service.Store;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 菜单管理（若依契约：经 registerProxyController 代理注册，SOYS 自动补 /api 全局前缀
 * → 实际路由 /api/prod-api/system/menu/*）：
 * 展示「内置菜单 ⊕ 插件登记菜单 ⊕ 自定义菜单(erp_menu)」合成树；
 * 增删改仅作用于自定义菜单表。
 */
@RequestMapping("/prod-api/system/menu")
public class SysMenuController {

    private final EripRegistry registry;

    public SysMenuController(EripRegistry registry) {
        this.registry = registry;
    }

    @ApiName("菜单列表")
    @ApiPermission("system:menu:list")
    @GetMapping("/list")
    public AjaxResult list() {
        return AjaxResult.success(buildMenuTree());
    }

    @ApiName("菜单树选择")
    @ApiPermission("system:menu:list")
    @GetMapping("/treeselect")
    public AjaxResult treeselect() {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> node : buildMenuTree()) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", node.get("menuId"));
            t.put("label", node.get("menuName"));
            t.put("children", toTreeselect((List<?>) node.get("children")));
            tree.add(t);
        }
        return AjaxResult.success(tree);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> toTreeselect(List<?> children) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : children == null ? new ArrayList<>() : children) {
            Map<String, Object> c = (Map<String, Object>) o;
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", c.get("menuId"));
            t.put("label", c.get("menuName"));
            t.put("children", toTreeselect((List<?>) c.get("children")));
            out.add(t);
        }
        return out;
    }

    @ApiName("菜单详情")
    @ApiPermission("system:menu:query")
    @GetMapping("/{menuId}")
    public AjaxResult detail(@PathVariable(name = "menuId") String menuId) {
        SysMenu m = Store.get(SysMenu.class, menuId);
        if (m != null) {
            return AjaxResult.success(m);
        }
        // 内置/插件菜单为代码声明，无实体记录——返回 404 语义由前端提示
        return AjaxResult.error("仅可查看自定义菜单（内置/插件菜单为代码声明）");
    }

    @ApiName("新增菜单")
    @ApiPermission("system:menu:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody String body) {
        Map<String, Object> map = com.github.cocosoys.mc.mcerp.util.Json.parseObject(body);
        SysMenu m = new SysMenu();
        m.setMenuId(UUID.randomUUID().toString());
        m.setParentId(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "parentId", "0"));
        m.setMenuName(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "menuName", ""));
        m.setOrderNum(com.github.cocosoys.mc.mcerp.util.Json.getInt(map, "orderNum", 0));
        m.setPath(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "path", ""));
        m.setComponent(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "component", ""));
        m.setMenuType(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "menuType", "C"));
        m.setPerms(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "perms", ""));
        m.setIcon(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "icon", ""));
        m.setVisible(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "visible", "0"));
        m.setStatus(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "status", "0"));
        if (m.getMenuName().isEmpty()) {
            return AjaxResult.error("菜单名称不能为空");
        }
        Store.insert(m);
        SysUserController.recordOper("菜单管理", "新增菜单", m.getMenuName(), "新增成功");
        return AjaxResult.success("新增成功");
    }

    @ApiName("编辑菜单")
    @ApiPermission("system:menu:edit")
    @PutMapping("/{menuId}")
    public AjaxResult update(@PathVariable(name = "menuId") String menuId, @RequestBody String body) {
        Map<String, Object> map = com.github.cocosoys.mc.mcerp.util.Json.parseObject(body);
        SysMenu m = menuId == null || menuId.isEmpty() ? null : Store.get(SysMenu.class, menuId);
        if (m == null) {
            return AjaxResult.error("仅可编辑自定义菜单（内置/插件菜单为代码声明）");
        }
        m.setParentId(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "parentId", m.getParentId()));
        m.setMenuName(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "menuName", m.getMenuName()));
        m.setOrderNum(com.github.cocosoys.mc.mcerp.util.Json.getInt(map, "orderNum", m.getOrderNum()));
        m.setPath(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "path", m.getPath()));
        m.setComponent(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "component", m.getComponent()));
        m.setMenuType(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "menuType", m.getMenuType()));
        m.setPerms(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "perms", m.getPerms()));
        m.setIcon(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "icon", m.getIcon()));
        m.setVisible(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "visible", m.getVisible()));
        m.setStatus(com.github.cocosoys.mc.mcerp.util.Json.getString(map, "status", m.getStatus()));
        Store.updateById(m);
        SysUserController.recordOper("菜单管理", "修改菜单", m.getMenuName(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @ApiName("删除菜单")
    @ApiPermission("system:menu:remove")
    @DeleteMapping("/{menuIds}")
    public AjaxResult remove(@PathVariable(name = "menuIds") String menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return AjaxResult.error("缺少 menuId");
        }
        for (String id : menuIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysMenu m = Store.get(SysMenu.class, id.trim());
            if (m == null) {
                continue;
            }
            Store.deleteById(SysMenu.class, id.trim());
            SysUserController.recordOper("菜单管理", "删除菜单", m.getMenuName(), "删除成功");
        }
        return AjaxResult.success("删除成功");
    }

    @ApiName("菜单排序")
    @ApiPermission("system:menu:edit")
    @PutMapping("/updateSort")
    public AjaxResult updateSort(@RequestBody String body) {
        // 自定义菜单排序（简化：仅回执成功）
        return AjaxResult.success("操作成功");
    }

    @ApiName("角色菜单树")
    @ApiPermission("system:menu:list")
    @GetMapping("/roleMenuTreeselect/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable(name = "roleId") String roleId) {
        return treeselect();
    }

    // ===== 合成树 =====

    public List<Map<String, Object>> buildMenuTree() {
        List<Map<String, Object>> top = new ArrayList<>();
        // 1. 内置
        top.add(node("system", "0", "系统管理", "M", "/system", "Layout", "", "system", 1));
        top.add(node("user", "system", "用户管理", "C", "user", "system/user/index", "system:user:list", "user", 1));
        top.add(node("menu", "system", "菜单管理", "C", "menu", "system/menu/index", "system:menu:list", "menu", 2));
        top.add(node("dict", "system", "字典管理", "C", "dict", "system/dict/index", "system:dict:list", "dict", 3));
        top.add(node("config", "system", "参数设置", "C", "config", "system/config/index", "system:config:list", "config", 4));
        top.add(node("notice", "system", "通知公告", "C", "notice", "system/notice/index", "system:notice:list", "notice", 5));
        top.add(node("log", "system", "日志管理", "M", "log", "Layout", "", "log", 6));
        top.add(node("operlog", "log", "操作日志", "C", "operlog", "monitor/operlog/index", "monitor:operlog:list", "operlog", 1));
        top.add(node("logininfor", "log", "登录日志", "C", "logininfor", "monitor/logininfor/index", "monitor:logininfor:list", "logininfor", 2));
        // 2. 插件登记模块
        for (EripModule m : registry.getModules()) {
            Map<String, Object> mod = node(m.getId(), "0", m.getDisplayName(), "M", "/" + m.getId().toLowerCase(),
                    "Layout", m.getPermission(), m.getIcon() == null ? "link" : m.getIcon(), 100 + m.getSortOrder());
            mod.put("children", menuNodes(m.getChildren(), m.getId()));
            top.add(mod);
        }
        // 3. 自定义菜单（erp_menu）
        List<SysMenu> customs = Store.select(SysMenu.class);
        customs.sort(Comparator.comparingInt(SysMenu::getOrderNum));
        for (SysMenu c : customs) {
            if ("0".equals(c.getParentId()) || "".equals(c.getParentId()) || c.getParentId() == null) {
                Map<String, Object> cn = sysMenuNode(c);
                cn.put("children", customChildren(customs, c.getMenuId()));
                top.add(cn);
            }
        }
        return top;
    }

    private static List<Map<String, Object>> customChildren(List<SysMenu> all, String parentId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysMenu c : all) {
            if (parentId != null && parentId.equals(c.getParentId())) {
                Map<String, Object> cn = sysMenuNode(c);
                cn.put("children", customChildren(all, c.getMenuId()));
                out.add(cn);
            }
        }
        return out;
    }

    private static Map<String, Object> sysMenuNode(SysMenu c) {
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
        n.put("children", new ArrayList<>());
        return n;
    }

    private List<Map<String, Object>> menuNodes(List<EripMenu> menus, String parentId) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (menus == null) {
            return out;
        }
        for (EripMenu m : menus) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("menuId", parentId + "_" + (m.getId() == null ? m.getTitle() : m.getId()));
            n.put("parentId", parentId);
            n.put("menuName", m.getTitle());
            n.put("orderNum", m.getSortOrder());
            n.put("path", m.getPath() == null ? m.getId() : m.getPath());
            n.put("component", "iframe:" + (m.getUrl() == null ? "" : m.getUrl()));
            n.put("menuType", m.getType() == null ? "C" : m.getType());
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
