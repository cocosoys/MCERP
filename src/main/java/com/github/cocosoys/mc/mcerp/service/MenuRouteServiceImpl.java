package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.EripMenu;
import com.github.cocosoys.mc.mcerp.EripModule;
import com.github.cocosoys.mc.mcerp.EripRegistry;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * getRouters 合成实现：内置菜单（6 模块）⊕ 已登记 ERP 模块菜单。
 * <ul>
 *   <li>内置：若依标准树，节点带 perms，无权限则过滤</li>
 *   <li>插件模块：主模块菜单 = 插件「展示名称」（顶层）；无子菜单时主模块本身即按钮（iframe 打开 homeUrl）</li>
 *   <li>子菜单/按钮：M 目录 / C 菜单（component=iframe:&lt;url&gt;）/ F 按钮（仅权限，不进路由）</li>
 * </ul>
 */
public class MenuRouteServiceImpl implements MenuRouteService {

    private final EripRegistry registry;
    private final AuthService auth;

    public MenuRouteServiceImpl(EripRegistry registry, AuthService auth) {
        this.registry = registry;
        this.auth = auth;
    }

    @Override
    public List<Map<String, Object>> buildRoutes(CredentialPresentation credential) {
        List<Map<String, Object>> routes = new ArrayList<>();
        // 内置树（带权限过滤）
        for (Map<String, Object> route : BuiltinMenus.builtinRoutes()) {
            Map<String, Object> filtered = filterRoute(route, credential);
            if (filtered != null) {
                routes.add(filtered);
            }
        }
        // 插件模块树
        for (EripModule module : registry.getModules()) {
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

    // ===== 内置树过滤 =====

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterRoute(Map<String, Object> route, CredentialPresentation credential) {
        String perms = (String) route.get("perms");
        if (perms != null && !perms.isEmpty() && !auth.hasPermission(credential, perms)) {
            return null;
        }
        List<Map<String, Object>> children = (List<Map<String, Object>>) route.get("children");
        List<Map<String, Object>> kept = new ArrayList<>();
        if (children != null) {
            for (Map<String, Object> child : children) {
                Map<String, Object> f = filterRoute(child, credential);
                if (f != null) {
                    kept.add(f);
                }
            }
        }
        route.put("children", kept);
        return route;
    }

    // ===== 插件模块路由 =====

    private Map<String, Object> moduleRoute(EripModule module, CredentialPresentation credential) {
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

    private List<Map<String, Object>> menuRoutes(List<EripMenu> menus, String parentName,
                                                 CredentialPresentation credential) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (menus == null) {
            return list;
        }
        for (EripMenu menu : menus) {
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

    private Map<String, Object> menuRoute(EripMenu menu, String parentName, CredentialPresentation credential) {
        String type = menu.getType() == null ? "C" : menu.getType();
        String name = parentName + "_" + safeName(menu.getId() == null ? menu.getTitle() : menu.getId());
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("name", name);
        route.put("path", menu.getPath() == null ? menu.getId() : menu.getPath());
        route.put("hidden", false);
        route.put("alwaysShow", true);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", menu.getTitle());
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
            route.put("component", iframeComponent(menu.getUrl()));
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

    private static boolean hasVisibleMenu(List<EripMenu> menus) {
        if (menus == null) {
            return false;
        }
        for (EripMenu m : menus) {
            if (m.isVisible() && !"F".equals(m.getType())) {
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
