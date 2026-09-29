package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.ErpRegistry;
import com.github.cocosoys.mc.mcerp.entity.ErpMenu;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpMenuVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpModuleVO;
import com.github.cocosoys.mc.mcerp.entity.vo.RouterMetaVO;
import com.github.cocosoys.mc.mcerp.entity.vo.RouterVO;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.MenuRouteService;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * getRouters 合成实现：菜单表（内置初始化数据 + 自定义 erp_menu）⊕ 已登记 ERP 模块菜单。
 */
public class MenuRouteServiceImpl implements MenuRouteService {

    private final ErpRegistry registry;
    private final AuthService auth;

    public MenuRouteServiceImpl(ErpRegistry registry, AuthService auth) {
        this.registry = registry;
        this.auth = auth;
    }

    @Override
    public List<RouterVO> buildRoutes(CredentialPresentation credential) {
        List<RouterVO> routes = new ArrayList<>();
        List<ErpMenu> menus = DATA.select(ErpMenu.class);
        menus.sort(Comparator.comparingInt(ErpMenu::getOrderNum));
        for (ErpMenu m : menus) {
            if (isTopLevel(m)) {
                RouterVO r = routeFromMenu(m, menus, credential);
                if (r != null) {
                    routes.add(r);
                }
            }
        }
        for (ErpModuleVO module : registry.getModules()) {
            if (!auth.hasPermission(credential, module.getPermission())) {
                continue;
            }
            RouterVO top = moduleRoute(module, credential);
            if (top != null) {
                routes.add(top);
            }
        }
        return routes;
    }

    private static boolean isTopLevel(ErpMenu m) {
        return m.getParentId() == null || m.getParentId() == 0;
    }

    private RouterVO routeFromMenu(ErpMenu menu, List<ErpMenu> all,
                                   CredentialPresentation credential) {
        if ("1".equals(menu.getVisible())) {
            return null;
        }
        if ("F".equals(menu.getMenuType())) {
            return null;
        }
        if (menu.getPerms() != null && !menu.getPerms().isEmpty()
                && !auth.hasPermission(credential, menu.getPerms())) {
            return null;
        }
        String path = menu.getPath() == null ? "" : menu.getPath();
        boolean frame = "0".equals(menu.getIsFrame());
        String routePath = frame
                ? path
                : (menu.getQuery() != null && !menu.getQuery().isEmpty() ? path + "?" + menu.getQuery() : path);

        RouterVO route = new RouterVO();
        route.setName(routeNameOf(menu, path));
        route.setPath(routePath);
        route.setHidden(false);
        route.setRedirect("noRedirect");
        route.setAlwaysShow("M".equals(menu.getMenuType()));

        boolean topLevel = isTopLevel(menu);
        if ("M".equals(menu.getMenuType()) && !topLevel) {
            route.setComponent("ParentView");
        } else {
            route.setComponent(menu.getComponent() == null || menu.getComponent().isEmpty()
                    ? "Layout" : menu.getComponent());
        }

        RouterMetaVO meta = new RouterMetaVO();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon() == null || menu.getIcon().isEmpty() ? "form" : menu.getIcon());
        meta.setNoCache("1".equals(menu.getIsCache()));
        meta.setLink(frame ? path : null);
        route.setMeta(meta);

        route.setChildren(menuChildren(menu.getMenuId(), all, credential));
        return route;
    }

    private static String routeNameOf(ErpMenu menu, String path) {
        if (menu.getRouteName() != null && !menu.getRouteName().isEmpty()) {
            return menu.getRouteName();
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return "Link";
        }
        return routeName(path);
    }

    private List<RouterVO> menuChildren(Long parentId, List<ErpMenu> all,
                                       CredentialPresentation credential) {
        List<RouterVO> list = new ArrayList<>();
        for (ErpMenu m : all) {
            if (parentId != null && parentId.equals(m.getParentId())) {
                RouterVO r = routeFromMenu(m, all, credential);
                if (r != null) {
                    list.add(r);
                }
            }
        }
        return list;
    }

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

    private RouterVO moduleRoute(ErpModuleVO module, CredentialPresentation credential) {
        String id = safeName(module.getId());
        boolean hasChildren = hasVisibleMenu(module.getChildren());
        RouterVO route = new RouterVO();
        route.setName(id);
        route.setPath("/" + id.toLowerCase());
        route.setHidden(false);
        route.setRedirect("noRedirect");
        route.setAlwaysShow(true);
        if (hasChildren) {
            route.setComponent("Layout");
        } else {
            route.setComponent(urlComponent(module.getHomeUrl(), module.getComponentMode()));
        }
        RouterMetaVO meta = new RouterMetaVO();
        meta.setTitle(module.getDisplayName());
        meta.setIcon(module.getIcon() == null ? "link" : module.getIcon());
        meta.setNoCache(false);
        meta.setLink(null);
        route.setMeta(meta);
        route.setChildren(menuRoutes(module.getChildren(), id, credential, module.getComponentMode()));
        return route;
    }

    private List<RouterVO> menuRoutes(List<ErpMenuVO> menus, String parentName,
                                      CredentialPresentation credential, String mode) {
        List<RouterVO> list = new ArrayList<>();
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
            RouterVO r = menuRoute(menu, parentName, credential, mode);
            if (r != null) {
                list.add(r);
            }
        }
        return list;
    }

    private RouterVO menuRoute(ErpMenuVO menu, String parentName, CredentialPresentation credential, String mode) {
        String type = menu.getMenuType() == null ? "C" : menu.getMenuType();
        String name = parentName + "_" + safeName(menu.getMenuId() == null ? menu.getMenuName() : menu.getMenuId());
        RouterVO route = new RouterVO();
        route.setName(name);
        route.setPath(menu.getPath() == null ? menu.getMenuId() : menu.getPath());
        route.setHidden(false);
        route.setAlwaysShow("M".equals(type));

        RouterMetaVO meta = new RouterMetaVO();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon() == null ? "form" : menu.getIcon());
        meta.setNoCache(false);
        meta.setLink(null);
        route.setMeta(meta);

        if ("M".equals(type)) {
            route.setRedirect("noRedirect");
            route.setComponent("Layout");
            route.setChildren(menuRoutes(menu.getChildren(), name, credential, mode));
        } else if ("F".equals(type)) {
            return null;
        } else {
            route.setRedirect("noRedirect");
            route.setComponent(urlComponent(menu.getComponent(), mode));
            route.setChildren(new ArrayList<>());
        }
        return route;
    }

    private static String urlComponent(String url, String mode) {
        if (url == null || url.isEmpty()) {
            url = "/";
        }
        return "WUJIE".equals(mode) ? "wujie:" + url : "iframe:" + url;
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
