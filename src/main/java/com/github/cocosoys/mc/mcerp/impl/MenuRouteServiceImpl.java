package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.ErpRegistry;
import com.github.cocosoys.mc.mcerp.entity.ErpMenu;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpMenuVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpModuleVO;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.MenuRouteService;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * getRouters 合成实现：菜单表（内置初始化数据 + 自定义 erp_menu）⊕ 已登记 ERP 模块菜单。
 * <ul>
 *   <li>内置/自定义：从 erp_menu 表读取（初始化数据见 resources/data/erp_menu.yml），
 *       目录 M / 菜单 C 生成路由（name 按若依规则 = path 首字母大写），按钮 F 仅权限标识不进路由；
 *       节点带 perms 且无权限则过滤</li>
 *   <li>插件模块：主模块菜单 = 插件「展示名称」（顶层）；无子菜单时主模块本身即按钮（iframe 打开 homeUrl）</li>
 *   <li>子菜单/按钮：M 目录 / C 菜单（component=iframe:&lt;url&gt;）/ F 按钮（仅权限，不进路由）</li>
 * </ul>
 */
public class MenuRouteServiceImpl implements MenuRouteService {

    private final ErpRegistry registry;
    private final AuthService auth;

    public MenuRouteServiceImpl(ErpRegistry registry, AuthService auth) {
        this.registry = registry;
        this.auth = auth;
    }

    @Override
    public List<Map<String, Object>> buildRoutes(CredentialPresentation credential) {
        List<Map<String, Object>> routes = new ArrayList<>();
        // 内置 + 自定义菜单（统一由 erp_menu 表驱动，含 resources 初始化数据）
        List<ErpMenu> menus = DATA.select(ErpMenu.class);
        menus.sort(Comparator.comparingInt(ErpMenu::getOrderNum));
        for (ErpMenu m : menus) {
            if (isTopLevel(m)) {
                Map<String, Object> r = routeFromMenu(m, menus, credential);
                if (r != null) {
                    routes.add(r);
                }
            }
        }
        // 插件模块树
        for (ErpModuleVO module : registry.getModules()) {
            if (!auth.hasPermission(credential, module.getPermission())) {
                continue; // 模块级权限
            }
            Map<String, Object> top = moduleRoute(module, credential);
            if (top != null) {
                routes.add(top);
            }
        }
        return routes;
    }

    // ===== 菜单表路由（内置 + 自定义） =====

    private static boolean isTopLevel(ErpMenu m) {
        return m.getParentId() == null || m.getParentId().isEmpty() || "0".equals(m.getParentId());
    }

    private Map<String, Object> routeFromMenu(ErpMenu menu, List<ErpMenu> all,
                                              CredentialPresentation credential) {
        if ("1".equals(menu.getVisible())) {
            return null;
        }
        if ("F".equals(menu.getMenuType())) {
            return null; // 按钮：仅权限标识，不进路由
        }
        if (menu.getPerms() != null && !menu.getPerms().isEmpty()
                && !auth.hasPermission(credential, menu.getPerms())) {
            return null;
        }
        String path = menu.getPath() == null ? "" : menu.getPath();
        boolean frame = "0".equals(menu.getIsFrame()); // 外链：path 为完整 URL，不拼 query
        String routePath = frame
                ? path
                : (menu.getQuery() != null && !menu.getQuery().isEmpty() ? path + "?" + menu.getQuery() : path);
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("name", routeNameOf(menu, path));
        route.put("path", routePath);
        route.put("hidden", false);
        route.put("redirect", "noRedirect");
        // 仅目录（M）强制 alwaysShow（前端据此渲染为可展开 el-submenu）；
        // C 菜单不设（undefined）→ 前端走 el-menu-item 直接跳转，否则叶子菜单会被渲染成空 submenu 无法点击
        route.put("alwaysShow", "M".equals(menu.getMenuType()));
        // 顶级 M 目录用 Layout（整页框架）；二级及以下 M 目录用 ParentView（纯 <router-view> 容器），
        // 否则 Layout 嵌套 Layout 会重复渲染 sidebar/navbar/tags-view（顶栏菜单异常）。
        boolean topLevel = isTopLevel(menu);
        if ("M".equals(menu.getMenuType()) && !topLevel) {
            route.put("component", "ParentView");
        } else {
            route.put("component", menu.getComponent() == null || menu.getComponent().isEmpty()
                    ? "Layout" : menu.getComponent());
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", menu.getMenuName());
        meta.put("icon", menu.getIcon() == null || menu.getIcon().isEmpty() ? "form" : menu.getIcon());
        meta.put("noCache", "1".equals(menu.getIsCache()));
        meta.put("link", frame ? path : null);
        route.put("meta", meta);
        route.put("children", menuChildren(menu.getMenuId(), all, credential));
        return route;
    }

    /** 路由 name：route_name 非空优先；外链占位 Link；否则按 path 首字母大写兜底。 */
    private static String routeNameOf(ErpMenu menu, String path) {
        if (menu.getRouteName() != null && !menu.getRouteName().isEmpty()) {
            return menu.getRouteName();
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return "Link"; // 外链不参与路由匹配，name 仅占位
        }
        return routeName(path);
    }

    private List<Map<String, Object>> menuChildren(String parentId, List<ErpMenu> all,
                                                   CredentialPresentation credential) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ErpMenu m : all) {
            if (parentId != null && parentId.equals(m.getParentId())) {
                Map<String, Object> r = routeFromMenu(m, all, credential);
                if (r != null) {
                    list.add(r);
                }
            }
        }
        return list;
    }

    /** 若依路由 name 规则：path 首字母大写（如 /system → System、user → User） */
    private static String routeName(String path) {
        String p = path;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.isEmpty()) {
            return "Index";
        }
        return Character.toUpperCase(p.charAt(0)) + p.substring(1);
    }

    // ===== 插件模块路由 =====

    private Map<String, Object> moduleRoute(ErpModuleVO module, CredentialPresentation credential) {
        String id = safeName(module.getId());
        boolean hasChildren = hasVisibleMenu(module.getChildren());
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("name", id);
        route.put("path", "/" + id.toLowerCase());
        route.put("hidden", false);
        route.put("redirect", "noRedirect");
        route.put("alwaysShow", true);
        if (hasChildren) {
            route.put("component", "Layout");
        } else {
            // 主模块菜单兼按钮：直接 iframe 打开 homeUrl
            route.put("component", iframeComponent(module.getHomeUrl()));
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", module.getDisplayName());
        meta.put("icon", module.getIcon() == null ? "link" : module.getIcon());
        meta.put("noCache", false);
        meta.put("link", null);
        route.put("meta", meta);
        route.put("children", menuRoutes(module.getChildren(), id, credential));
        return route;
    }

    private List<Map<String, Object>> menuRoutes(List<ErpMenuVO> menus, String parentName,
                                                 CredentialPresentation credential) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (menus == null) {
            return list;
        }
        for (ErpMenuVO menu : menus) {
            if (!menu.isVisible()) {
                continue;
            }
            if (menu.getPerms() != null && !menu.getPerms().isEmpty()
                    && !auth.hasPermission(credential, menu.getPerms())) {
                continue;
            }
            Map<String, Object> r = menuRoute(menu, parentName, credential);
            if (r != null) {
                list.add(r);
            }
        }
        return list;
    }

    private Map<String, Object> menuRoute(ErpMenuVO menu, String parentName, CredentialPresentation credential) {
        String type = menu.getMenuType() == null ? "C" : menu.getMenuType();
        String name = parentName + "_" + safeName(menu.getMenuId() == null ? menu.getMenuName() : menu.getMenuId());
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("name", name);
        route.put("path", menu.getPath() == null ? menu.getMenuId() : menu.getPath());
        route.put("hidden", false);
        // 与菜单表路径 routeFromMenu 保持一致：仅 M 目录 alwaysShow=true，C 叶子必须 false，
        // 否则前端把叶子菜单渲染成空 submenu（带展开箭头但 children=[]），点击无法跳转 iframe。
        route.put("alwaysShow", "M".equals(type));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", menu.getMenuName());
        meta.put("icon", menu.getIcon() == null ? "form" : menu.getIcon());
        meta.put("noCache", false);
        meta.put("link", null);
        route.put("meta", meta);
        if ("M".equals(type)) {
            // 目录：Layout + 子项
            route.put("redirect", "noRedirect");
            route.put("component", "Layout");
            route.put("children", menuRoutes(menu.getChildren(), name, credential));
        } else if ("F".equals(type)) {
            // 按钮：仅权限标识，不进路由
            return null;
        } else {
            // C 菜单：iframe 打开目标页面
            route.put("redirect", "noRedirect");
            route.put("component", iframeComponent(menu.getComponent()));
            route.put("children", new ArrayList<>());
        }
        return route;
    }

    /**
     * iframe 组件路径：绝对 URL 直接用，相对路径补 /plugins/ 前缀语义由插件自身页面地址保证。
     */
    private static String iframeComponent(String url) {
        if (url == null || url.isEmpty()) {
            return "iframe:/";
        }
        return "iframe:" + url;
    }

    private static boolean hasVisibleMenu(List<ErpMenuVO> menus) {
        if (menus == null) {
            return false;
        }
        for (ErpMenuVO m : menus) {
            if (m.isVisible() && !"F".equals(m.getMenuType())) {
                return true;
            }
        }
        return false;
    }

    private static String safeName(String s) {
        if (s == null) {
            return "item";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "item" : sb.toString();
    }
}
