package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.controller.AuthController;
import com.github.cocosoys.mc.mcerp.controller.SysConfigController;
import com.github.cocosoys.mc.mcerp.controller.SysDictController;
import com.github.cocosoys.mc.mcerp.controller.SysLogController;
import com.github.cocosoys.mc.mcerp.controller.SysMenuController;
import com.github.cocosoys.mc.mcerp.controller.SysNoticeController;
import com.github.cocosoys.mc.mcerp.controller.SysUserController;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.AuthServiceImpl;
import com.github.cocosoys.mc.mcerp.service.MenuRouteService;
import com.github.cocosoys.mc.mcerp.service.MenuRouteServiceImpl;
import com.github.cocosoys.mc.mcerp.web.DistHost;
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

/**
 * MCERP — 统一 ERP 后台中控（Spigot 1.12.2，软依赖 SOYSHTTPOverMC）。
 * <ul>
 *   <li>复用 dist 内 RuoYi-Vue 前端（登录/主题/iframe 原生支持）</li>
 *   <li>后端：SOYS 注解式 API（控制器 registerProxyController 代理注册 → 无 /plugins/MCERP 前缀；
 *       SOYS 注册时自动补全局前缀 /api → 实际路由 /api/prod-api/*，匹配 RuoYi axios baseURL）</li>
 *   <li>鉴权：完全交给 SOYS 登录桥（AuthMe）+ 会话令牌 + Bukkit 权限镜像</li>
 *   <li>存储：SQL/YAML 双兼容（dlz 实体注解，仅需正常书写实体类）</li>
 * </ul>
 */
@CustomLog
public class MCERP extends JavaPlugin implements Listener {

    private static MCERP instance;

    private EripRegistry registry;
    private McerpApi api;
    private AuthService authService;

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

        // 1. 注册若依契约控制器（registerProxyController：代理注册、无 /plugins/MCERP 前缀；
        //    SOYS ApiRegistry 注册时自动 applyPrefix 全局前缀（默认 /api）→ 注册表路由 /api/prod-api/*，
        //    与前端 baseURL（/api/prod-api）对齐）
        MenuRouteService routeService = new MenuRouteServiceImpl(registry, authService);
        soysApi.getApiRegistration().registerProxyController(new AuthController(authService, routeService));
        soysApi.getApiRegistration().registerProxyController(new SysUserController(authService));
        soysApi.getApiRegistration().registerProxyController(new SysMenuController(registry));
        SysDictController dictController = new SysDictController();
        soysApi.getApiRegistration().registerProxyController(dictController);
        soysApi.getApiRegistration().registerProxyController(new SysConfigController());
        soysApi.getApiRegistration().registerProxyController(new SysNoticeController());
        soysApi.getApiRegistration().registerProxyController(new SysLogController());

        // 2. 托管 RuoYi 前端 dist
        DistHost.host(this, soysApi);

        // 3. 初始化内置字典与参数
        dictController.ensureBuiltinDicts();
        new SysConfigController().ensureBuiltinConfigs();

        soysInited = true;
        log.info("SOYS 接入完成：/prod-api 契约 API + dist 托管");
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
            for (EripModule m : registry.getModules()) {
                sender.sendMessage("§7  - §f" + m.getId() + " §8(" + m.getDisplayName() + ")");
            }
            return true;
        }
        sender.sendMessage("§a[MCERP] 用法: /mcerp reload | /mcerp list");
        return true;
    }
}
