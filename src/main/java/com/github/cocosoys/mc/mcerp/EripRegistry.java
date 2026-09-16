package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.entity.vo.EripMenuVO;
import com.github.cocosoys.mc.mcerp.entity.vo.EripModuleVO;
import lombok.CustomLog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 模块注册中心。
 * <ul>
 *   <li>动态通道：其他插件 onEnable 调 {@link #registerModule(JavaPlugin, EripModuleVO)}（首选）</li>
 *   <li>静态通道：plugins/MCERP/erp-modules.yml（第三方非 SOYS 系插件，不写代码即可登记）</li>
 *   <li>持久化：动态登记结果快照到 erp-registry.yml，/mcerp reload 以「静态配置 + 持久化快照 + 当前在线注册」重建</li>
 * </ul>
 * 合并优先级（同 id）：动态在线注册 &gt; erp-modules.yml 静态配置 &gt; erp-registry.yml 持久化快照。
 */
@CustomLog
public class EripRegistry {

    private static final String STATIC_YML = "erp-modules.yml";
    private static final String SNAPSHOT_YML = "erp-registry.yml";

    private final JavaPlugin plugin;

    /** 在线动态注册：id -> (module, ownerPluginName) */
    private final Map<String, Registered> dynamic = new LinkedHashMap<>();

    /** erp-modules.yml 静态配置 */
    private final Map<String, EripModuleVO> staticModules = new LinkedHashMap<>();

    /** erp-registry.yml 持久化快照 */
    private final Map<String, EripModuleVO> persisted = new LinkedHashMap<>();

    private static final class Registered {
        final EripModuleVO module;
        final String owner;

        Registered(EripModuleVO module, String owner) {
            this.module = module;
            this.owner = owner;
        }
    }

    public EripRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 动态登记（其他插件 onEnable 调用）。同 id 重复登记会被覆盖（新的在线状态优先）。
     */
    public synchronized void registerModule(JavaPlugin owner, EripModuleVO module) {
        if (module == null || module.getId() == null || module.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("EripModule.id 不能为空");
        }
        if (module.getDisplayName() == null || module.getDisplayName().trim().isEmpty()) {
            throw new IllegalArgumentException("EripModule.displayName 不能为空（展示名称必填）");
        }
        module.setId(module.getId().trim());
        dynamic.put(module.getId(), new Registered(module, owner == null ? "?" : owner.getName()));
        persistSnapshot();
        log.info("ERP 模块已登记: " + module.getId() + " (展示名: " + module.getDisplayName() + ")");
    }

    /**
     * 按插件名摘除其全部动态登记（插件禁用时调用）。
     */
    public synchronized void unregisterModule(String ownerPluginName) {
        boolean changed = false;
        java.util.Iterator<Map.Entry<String, Registered>> it = dynamic.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Registered> e = it.next();
            if (e.getValue().owner.equalsIgnoreCase(ownerPluginName)) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            log.info("插件禁用，摘除其 ERP 登记: " + ownerPluginName);
        }
    }

    /**
     * 重载：重读 erp-modules.yml 与 erp-registry.yml（保留当前在线注册）。
     */
    public synchronized void reload() {
        staticModules.clear();
        persisted.clear();
        loadStatic();
        loadPersisted();
        log.info("注册中心已重载: 静态 " + staticModules.size()
                + " / 持久化 " + persisted.size() + " / 在线 " + dynamic.size());
    }

    /**
     * 合并后的模块列表（按 sortOrder 升序）。
     */
    public synchronized List<EripModuleVO> getModules() {
        Map<String, EripModuleVO> merged = new LinkedHashMap<>();
        merged.putAll(staticModules);
        merged.putAll(persisted);
        for (Registered r : dynamic.values()) {
            merged.put(r.module.getId(), r.module);
        }
        List<EripModuleVO> list = new ArrayList<>(merged.values());
        list.sort(Comparator.comparingInt(EripModuleVO::getSortOrder));
        return list;
    }

    public synchronized EripModuleVO getModule(String id) {
        for (EripModuleVO m : getModules()) {
            if (m.getId().equalsIgnoreCase(id)) {
                return m;
            }
        }
        return null;
    }

    public synchronized boolean isRegistered(String id) {
        return getModule(id) != null;
    }

    public synchronized List<String> getModuleIds() {
        List<String> ids = new ArrayList<>();
        for (EripModuleVO m : getModules()) {
            ids.add(m.getId());
        }
        return ids;
    }

    // ===== 静态配置（erp-modules.yml）=====

    private void loadStatic() {
        File f = new File(plugin.getDataFolder(), STATIC_YML);
        if (!f.exists()) {
            plugin.saveResource(STATIC_YML, false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        List<?> list = yml.getList("modules");
        if (list == null) {
            return;
        }
        for (Object o : list) {
            if (o instanceof Map) {
                EripModuleVO m = fromMap((Map<?, ?>) o);
                if (m != null && m.getId() != null) {
                    staticModules.put(m.getId(), m);
                }
            } else if (o instanceof ConfigurationSection) {
                EripModuleVO m = fromSection((ConfigurationSection) o);
                if (m != null && m.getId() != null) {
                    staticModules.put(m.getId(), m);
                }
            }
        }
    }

    // ===== 持久化快照（erp-registry.yml）=====

    private void loadPersisted() {
        File f = new File(plugin.getDataFolder(), SNAPSHOT_YML);
        if (!f.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        List<?> list = yml.getList("modules");
        if (list == null) {
            return;
        }
        for (Object o : list) {
            if (o instanceof Map) {
                EripModuleVO m = fromMap((Map<?, ?>) o);
                if (m != null && m.getId() != null) {
                    persisted.put(m.getId(), m);
                }
            } else if (o instanceof ConfigurationSection) {
                EripModuleVO m = fromSection((ConfigurationSection) o);
                if (m != null && m.getId() != null) {
                    persisted.put(m.getId(), m);
                }
            }
        }
    }

    private void persistSnapshot() {
        YamlConfiguration yml = new YamlConfiguration();
        List<Map<String, Object>> modules = new ArrayList<>();
        for (Registered r : dynamic.values()) {
            modules.add(toMap(r.module));
        }
        yml.set("modules", modules);
        try {
            yml.save(new File(plugin.getDataFolder(), SNAPSHOT_YML));
        } catch (IOException e) {
            log.warn("持久化 erp-registry.yml 失败: " + e.getMessage());
        }
    }

    // ===== Map / Section 转换 =====

    private static EripModuleVO fromMap(Map<?, ?> map) {
        EripModuleVO m = new EripModuleVO();
        m.setId(str(map.get("id")));
        m.setDisplayName(str(map.get("displayName")));
        m.setIcon(str(map.get("icon")));
        m.setHomeUrl(str(map.get("homeUrl")));
        m.setPermission(str(map.get("permission")));
        m.setSortOrder(intOf(map.get("sortOrder"), 0));
        Object children = map.get("children");
        if (children instanceof List) {
            for (Object c : (List<?>) children) {
                if (c instanceof Map) {
                    m.getChildren().add(fromMenuMap((Map<?, ?>) c));
                }
            }
        }
        return m.getId() == null ? null : m;
    }

    private static EripModuleVO fromSection(ConfigurationSection s) {
        EripModuleVO m = new EripModuleVO();
        m.setId(s.getString("id"));
        m.setDisplayName(s.getString("displayName"));
        m.setIcon(s.getString("icon"));
        m.setHomeUrl(s.getString("homeUrl"));
        m.setPermission(s.getString("permission"));
        m.setSortOrder(s.getInt("sortOrder", 0));
        ConfigurationSection children = s.getConfigurationSection("children");
        if (children != null) {
            for (String key : children.getKeys(false)) {
                ConfigurationSection cs = children.getConfigurationSection(key);
                if (cs != null) {
                    m.getChildren().add(fromMenuSection(key, cs));
                }
            }
        }
        return m.getId() == null ? null : m;
    }

    private static EripMenuVO fromMenuMap(Map<?, ?> map) {
        EripMenuVO menu = new EripMenuVO();
        menu.setId(str(map.get("id")));
        menu.setTitle(str(map.get("title")));
        menu.setIcon(str(map.get("icon")));
        menu.setType(str(map.get("type")));
        menu.setPath(str(map.get("path")));
        menu.setUrl(str(map.get("url")));
        menu.setPerms(str(map.get("perms")));
        menu.setSortOrder(intOf(map.get("sortOrder"), 0));
        Object visible = map.get("visible");
        menu.setVisible(visible == null || Boolean.parseBoolean(String.valueOf(visible)));
        Object children = map.get("children");
        if (children instanceof List) {
            for (Object c : (List<?>) children) {
                if (c instanceof Map) {
                    menu.getChildren().add(fromMenuMap((Map<?, ?>) c));
                }
            }
        }
        return menu;
    }

    private static EripMenuVO fromMenuSection(String key, ConfigurationSection s) {
        EripMenuVO menu = new EripMenuVO();
        menu.setId(s.getString("id", key));
        menu.setTitle(s.getString("title", key));
        menu.setIcon(s.getString("icon"));
        menu.setType(s.getString("type", "C"));
        menu.setPath(s.getString("path"));
        menu.setUrl(s.getString("url"));
        menu.setPerms(s.getString("perms"));
        menu.setSortOrder(s.getInt("sortOrder", 0));
        menu.setVisible(s.getBoolean("visible", true));
        ConfigurationSection children = s.getConfigurationSection("children");
        if (children != null) {
            for (String k : children.getKeys(false)) {
                ConfigurationSection cs = children.getConfigurationSection(k);
                if (cs != null) {
                    menu.getChildren().add(fromMenuSection(k, cs));
                }
            }
        }
        return menu;
    }

    private static Map<String, Object> toMap(EripModuleVO m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("displayName", m.getDisplayName());
        map.put("icon", m.getIcon());
        map.put("homeUrl", m.getHomeUrl());
        map.put("sortOrder", m.getSortOrder());
        map.put("permission", m.getPermission());
        List<Map<String, Object>> children = new ArrayList<>();
        for (EripMenuVO c : m.getChildren()) {
            children.add(toMenuMap(c));
        }
        map.put("children", children);
        return map;
    }

    private static Map<String, Object> toMenuMap(EripMenuVO menu) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", menu.getId());
        map.put("title", menu.getTitle());
        map.put("icon", menu.getIcon());
        map.put("type", menu.getType());
        map.put("path", menu.getPath());
        map.put("url", menu.getUrl());
        map.put("perms", menu.getPerms());
        map.put("sortOrder", menu.getSortOrder());
        map.put("visible", menu.isVisible());
        List<Map<String, Object>> children = new ArrayList<>();
        for (EripMenuVO c : menu.getChildren()) {
            children.add(toMenuMap(c));
        }
        map.put("children", children);
        return map;
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static int intOf(Object o, int def) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o != null) {
            try {
                return Integer.parseInt(String.valueOf(o).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }
}
