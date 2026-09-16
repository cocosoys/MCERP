package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.controller.AuthController;
import com.github.cocosoys.mc.mcerp.controller.SysConfigController;
import com.github.cocosoys.mc.mcerp.controller.SysDictController;
import com.github.cocosoys.mc.mcerp.controller.SysLogController;
import com.github.cocosoys.mc.mcerp.controller.SysMenuController;
import com.github.cocosoys.mc.mcerp.controller.SysNoticeController;
import com.github.cocosoys.mc.mcerp.controller.SysUserController;
import com.github.cocosoys.mc.mcerp.entity.vo.EripModuleVO;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.impl.AuthServiceImpl;
import com.github.cocosoys.mc.mcerp.service.MenuRouteService;
import com.github.cocosoys.mc.mcerp.impl.MenuRouteServiceImpl;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.impl.OperLogServiceImpl;
import com.github.cocosoys.mc.mcerp.service.SysUserService;
import com.github.cocosoys.mc.mcerp.impl.SysUserServiceImpl;
import com.github.cocosoys.mc.mcerp.service.SysMenuService;
import com.github.cocosoys.mc.mcerp.impl.SysMenuServiceImpl;
import com.github.cocosoys.mc.mcerp.service.SysDictService;
import com.github.cocosoys.mc.mcerp.impl.SysDictServiceImpl;
import com.github.cocosoys.mc.mcerp.service.SysConfigService;
import com.github.cocosoys.mc.mcerp.impl.SysConfigServiceImpl;
import com.github.cocosoys.mc.mcerp.service.SysNoticeService;
import com.github.cocosoys.mc.mcerp.impl.SysNoticeServiceImpl;
import com.github.cocosoys.mc.mcerp.service.SysLogService;
import com.github.cocosoys.mc.mcerp.impl.SysLogServiceImpl;
import com.github.cocosoys.mc.soyshttpovermc.HttpOverMcPlugin;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysHttpOverMcApi;
import com.github.cocosoys.mc.soyshttpovermc.api.event.SoysReadyEvent;
import lombok.CustomLog;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * MCERP — 统一 ERP 后台中控（Spigot 1.12.2，软依赖 SOYSHTTPOverMC）。
 * <ul>
 *   <li>复用 dist 内 RuoYi-Vue 前端（登录/主题/iframe 原生支持）</li>
 *   <li>后端：SOYS 注解式 API（控制器 registerController 正常登记 → 自动补 /api/plugins/MCERP 前缀；
 *       实际路由 /api/plugins/MCERP/<类级前缀>/*，前端 apiBaseURL 读契约 apiFullPrefix 对齐）</li>
 *   <li>鉴权：完全交给 SOYS 登录桥（AuthMe）+ 会话令牌 + Bukkit 权限镜像</li>
 *   <li>存储：SQL/YAML 双兼容（dlz 实体注解，仅需正常书写实体类）</li>
 * </ul>
 */
@CustomLog
public class MCERP extends JavaPlugin implements Listener {

    private static MCERP instance;

    private EripRegistry registry;
    private McerpApi api;
    private McerpExpansion expansion;
    private AuthService authService;
    private MenuRouteService menuRouteService;
    private OperLogService operLogService;
    private SysUserService sysUserService;
    private SysMenuService sysMenuService;
    private SysDictService sysDictService;
    private SysConfigService sysConfigService;
    private SysNoticeService sysNoticeService;
    private SysLogService sysLogService;

    private boolean soysInited = false;

    public static MCERP getInstance() {
        return instance;
    }

    /** 外部插件接入门面：McerpApi.getRegistry().registerModule(...) */
    public McerpApi getApi() {
        return api;
    }

    @Override
    public void onEnable() {
        instance = this;
        registry = new EripRegistry(this);
        authService = new AuthServiceImpl(registry);
        menuRouteService = new MenuRouteServiceImpl(registry, authService);
        operLogService = new OperLogServiceImpl();
        sysUserService = new SysUserServiceImpl(authService, operLogService);
        sysMenuService = new SysMenuServiceImpl(registry, operLogService);
        sysDictService = new SysDictServiceImpl(operLogService);
        sysConfigService = new SysConfigServiceImpl(operLogService);
        sysNoticeService = new SysNoticeServiceImpl(operLogService);
        sysLogService = new SysLogServiceImpl();
        api = new McerpApi() {
            @Override
            public EripRegistry getRegistry() {
                return registry;
            }
        };
        getServer().getPluginManager().registerEvents(this, this);
        // 插件禁用时摘除其 ERP 登记（生命周期自动摘除）
        if (HttpOverMcPlugin.getInstance() != null) {
            initSoys();
        } else {
            log.info("SOYSHTTPOverMC 尚未就绪，等待 SoysReadyEvent...");
        }
        log.info("已启用 (ERP 统一中控)");
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            expansion.unregister(); // SoysExpansion 精确反注册：批量端点 + expansion:MCERP 页面
        }
        log.info("已禁用");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSoysReady(SoysReadyEvent event) {
        if (!soysInited) {
            initSoys();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != this) {
            registry.unregisterModule(event.getPlugin().getName());
        }
    }

    private synchronized void initSoys() {
        if (soysInited) {
            return;
        }
        HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
        if (soys == null) {
            return;
        }
        SoysHttpOverMcApi soysApi = soys.getApi();

        //    API 注册：SoysExpansion 极简注册门户（McerpExpansion 覆写 registerController()
        //    批量登记全部 Controller 实例，正常登记自动补 /api/plugins/MCERP 前缀；
        //    resourceRoot()="dist" 由骨架自动托管前端页面，打 expansion:MCERP tag）
        if (expansion == null) {
            expansion = new McerpExpansion(soysApi, buildControllers());
        }
        if (!expansion.register()) {
            log.warn("McerpExpansion 注册失败：检查 identifier 冲突 / SOYS bootstrap 未就绪");
        }

        initData();

        soysInited = true;
        log.info("SOYS 接入完成：SoysExpansion 门户注册（" + (soysApi.getApiRegistration().getRegisteredApis() == null ? 0 : soysApi.getApiRegistration().getRegisteredApis().size()) + " 路由） + dist 托管（expansion:MCERP）");
    }

    /**
     * 记录所有 controller 实例化（供 McerpExpansion 批量注册；批量注册与注销共用同一来源）。
     */
    private List<Object> buildControllers() {
        List<Object> list = new ArrayList<>();
        list.add(new AuthController(authService, menuRouteService));
        list.add(new SysUserController(sysUserService));
        list.add(new SysMenuController(sysMenuService));
        list.add(new SysDictController(sysDictService));
        list.add(new SysConfigController(sysConfigService));
        list.add(new SysNoticeController(sysNoticeService));
        list.add(new SysLogController(sysLogService));
        return list;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            registry.reload();
            sender.sendMessage("§a[MCERP] 注册中心已重载");
            return true;
        }
        if (args.length > 0 && "list".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§a[MCERP] 已登记模块 (" + registry.getModules().size() + "):");
            for (EripModuleVO m : registry.getModules()) {
                sender.sendMessage("§7  - §f" + m.getId() + " §8(" + m.getDisplayName() + ")");
            }
            return true;
        }
        sender.sendMessage("§a[MCERP] 用法: /mcerp reload | /mcerp list");
        return true;
    }

    /**
     * 初始化表数据（YAML 存储模式）：把 resources/data/&lt;表名&gt;.yml 释放到 SOYS 数据目录
     * （plugins/SOYSHTTPOverMC/data，与 DATA 门面同一 dataDir），已存在文件不覆盖。
     * 覆盖 8 张表：erp_menu/erp_config/erp_dict_type/erp_dict_data/erp_user/erp_oper_log/erp_notice/erp_logininfor。
     */
    private void initData() {
        try {
            HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
            if (soys == null) {
                return;
            }
            File dataDir = new File(soys.getDataFolder(), "data");
            String[] tables = {
                "erp_menu", "erp_config", "erp_dict_type", "erp_dict_data",
                "erp_user", "erp_oper_log", "erp_notice", "erp_logininfor"
            };
            for (String t : tables) {
                File f = new File(dataDir, t + ".yml");
                if (f.exists()) {
                    continue;
                }
                try (InputStream in = getResource("data/" + t + ".yml")) {
                    if (in == null) {
                        log.warn("初始化表数据缺少内置文件: data/" + t + ".yml");
                        continue;
                    }
                    if (!dataDir.exists() && !dataDir.mkdirs()) {
                        log.warn("无法创建数据目录: " + dataDir);
                        return;
                    }
                    Files.copy(in, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log.info("初始化表数据: " + t);
                }
            }
        } catch (IOException e) {
            log.warn("初始化表数据失败: %s", e.getMessage());
        }
    }

}
