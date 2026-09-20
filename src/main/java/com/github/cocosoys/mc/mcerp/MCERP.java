package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.entity.vo.ErpModuleVO;
import com.github.cocosoys.mc.soyshttpovermc.HttpOverMcPlugin;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysExpansion;
import com.github.cocosoys.mc.soyshttpovermc.api.event.SoysReadyEvent;
import lombok.CustomLog;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
@Getter
public class MCERP extends JavaPlugin implements Listener {

    private @Getter static MCERP instance;

    private ErpRegistry registry;
    private McErpHostExpansion expansion;

    @Override
    public void onEnable() {
        instance = this;
        McerpExpansion.setMcerp(this); // 供附属 ERP 模块（McerpExpansion 子类）登记用
        registry = new ErpRegistry();
        getServer().getPluginManager().registerEvents(this, this);
        // 服务器完全启动后（所有插件 enable 完成）再扫描一次 ERP 模块索引兜底：
        Bukkit.getScheduler().runTaskLater(this, this::mcerpReload, 20L);
        // SOYS 可能已先启用（SoysReadyEvent 在监听器注册前已广播）→ 就绪则直接注册
        if (HttpOverMcPlugin.getInstance() != null) {
            registerSoys();
        }
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
        registerSoys();
    }

    /**
     * SOYS 接入：McErpHostExpansion 宿主门户（覆写 buildControllers() 记录全部 Controller
     * 实例，主插件 registerController() 自动批量登记补 /api/plugins/MCERP 前缀；
     * resourceRoot()="dist" 由骨架自动托管前端页面，打 expansion:MCERP tag）。
     * 幂等：已注册过则跳过；失败置空允许事件重试。
     */
    private void registerSoys() {
        if (expansion != null) {
            return;
        }
        expansion = new McErpHostExpansion(instance);
        if (!expansion.register()) {
            log.warn("McErpHostExpansion 注册失败：检查 identifier 冲突 / SOYS bootstrap 未就绪");
            expansion = null; // 失败置空，允许后续事件重试
            return;
        }
        // 注册主插件 reload 钩子：/soyshttp reload 时同步重登记 ERP 模块（与 /mcerp reload 双通道）
        HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
        if (soys != null && soys.getApi() != null) {
            soys.getApi().registerReloadHook(this::mcerpReload);
        }
        log.info("SOYS 接入完成：SoysExpansion 门户注册（路由） + dist 托管（expansion:MCERP）");
        log.info("已启用 (ERP 统一中控)");
    }

    /**
     * ERP 模块索引重建：以 {@code SoysExpansion.REGISTERED} 为准——
     * 先清理已不在 REGISTERED 的残留索引（reloadExpansions），
     * 再全量填充索引（registerModule，仅 identifier，已登记跳过，幂等）。
     * 由 /mcerp reload、/soyshttp reload（registerReloadHook）与启动扫描三处触发。
     */
    private void mcerpReload() {
        List<McerpExpansion> exps = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (SoysExpansion exp : SoysExpansion.registered().values()) {
            if (exp instanceof McerpExpansion) {
                exps.add((McerpExpansion) exp);
                ids.add(exp.getIdentifier());
            }
        }
        int removed = registry.reloadExpansions(ids);
        int added = 0;
        for (McerpExpansion e : exps) {
            added += registry.registerModule(e);
        }
        log.info("ERP 模块索引重建完成（移除残留 " + removed + "，新增 " + added + "）");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != this) {
            registry.unregisterModule(event.getPlugin().getName());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            mcerpReload(); // 以 SoysExpansion.REGISTERED 为准重建（清理残留 + 补登记），已无本地化文件可重载
            sender.sendMessage("§a[MCERP] 注册中心已重载");
            return true;
        }
        if (args.length > 0 && "list".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§a[MCERP] 已登记模块 (" + registry.getModules().size() + "):");
            for (ErpModuleVO m : registry.getModules()) {
                sender.sendMessage("§7  - §f" + m.getId() + " §8(" + m.getDisplayName() + ")");
            }
            return true;
        }
        sender.sendMessage("§a[MCERP] 用法: /mcerp reload | /mcerp list");
        return true;
    }

}
