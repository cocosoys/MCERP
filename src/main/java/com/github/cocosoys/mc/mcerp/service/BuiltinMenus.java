package com.github.cocosoys.mc.mcerp.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置菜单（仅实现用户点名的 6 个模块：用户/菜单/字典/参数/通知/日志）。
 * 结构仿若依标准菜单树；perms 与若依保持一致，供 getRouters 过滤与 getInfo 权限清单使用。
 */
public final class BuiltinMenus {

    private BuiltinMenus() {
    }

    /** 全部内置权限标识（用于 getInfo 逐项判定） */
    public static final List<String> ALL_PERMS = Arrays.asList(
            "system:user:list", "system:user:query", "system:user:add", "system:user:edit",
            "system:user:remove", "system:user:resetPwd", "system:user:changeStatus", "system:user:export",
            "system:menu:list", "system:menu:query", "system:menu:add", "system:menu:edit", "system:menu:remove",
            "system:dict:list", "system:dict:query", "system:dict:add", "system:dict:edit", "system:dict:remove",
            "system:config:list", "system:config:query", "system:config:add", "system:config:edit", "system:config:remove",
            "system:notice:list", "system:notice:query", "system:notice:add", "system:notice:edit", "system:notice:remove",
            "monitor:operlog:list", "monitor:operlog:query", "monitor:operlog:export", "monitor:operlog:remove", "monitor:operlog:clean",
            "monitor:logininfor:list", "monitor:logininfor:query", "monitor:logininfor:export",
            "monitor:logininfor:remove", "monitor:logininfor:clean");

    /**
     * 内置路由树（若依 getRouters 契约），perms 为 null 表示不校验（仅登录即可见）。
     */
    public static List<Map<String, Object>> builtinRoutes() {
        Map<String, Object> system = route("System", "/system", "Layout", "系统管理", "system", null,
                Arrays.asList(
                        route("User", "user", "system/user/index", "用户管理", "user", "system:user:list", null),
                        route("Menu", "menu", "system/menu/index", "菜单管理", "menu", "system:menu:list", null),
                        route("Dict", "dict", "system/dict/index", "字典管理", "dict", "system:dict:list", null),
                        route("Config", "config", "system/config/index", "参数设置", "config", "system:config:list", null),
                        route("Notice", "notice", "system/notice/index", "通知公告", "notice", "system:notice:list", null),
                        route("Log", "log", "Layout", "日志管理", "log", "system:notice:list",
                                Arrays.asList(
                                        route("Operlog", "operlog", "monitor/operlog/index", "操作日志", "operlog", "monitor:operlog:list", null),
                                        route("Logininfor", "logininfor", "monitor/logininfor/index", "登录日志", "logininfor", "monitor:logininfor:list", null)))
                ));
        return Arrays.asList(system);
    }

    private static Map<String, Object> route(String name, String path, String component, String title,
                                             String icon, String perms, List<Map<String, Object>> children) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("name", name);
        r.put("path", path);
        r.put("hidden", false);
        r.put("redirect", "noRedirect");
        r.put("component", component);
        r.put("alwaysShow", true);
        r.put("perms", perms);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", title);
        meta.put("icon", icon);
        meta.put("noCache", false);
        meta.put("link", null);
        r.put("meta", meta);
        r.put("children", children == null ? new java.util.ArrayList<>() : children);
        return r;
    }
}
