package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.controller.AuthController;
import com.github.cocosoys.mc.mcerp.controller.SysConfigController;
import com.github.cocosoys.mc.mcerp.controller.SysDictController;
import com.github.cocosoys.mc.mcerp.controller.SysLogController;
import com.github.cocosoys.mc.mcerp.controller.SysMenuController;
import com.github.cocosoys.mc.mcerp.controller.SysNoticeController;
import com.github.cocosoys.mc.mcerp.controller.SysUserController;
import com.github.cocosoys.mc.mcerp.impl.AuthServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.MenuRouteServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.OperLogServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.SysConfigServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.SysDictServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.SysLogServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.SysMenuServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.SysNoticeServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.SysUserServiceImpl;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.MenuRouteService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.SysConfigService;
import com.github.cocosoys.mc.mcerp.service.SysDictService;
import com.github.cocosoys.mc.mcerp.service.SysLogService;
import com.github.cocosoys.mc.mcerp.service.SysMenuService;
import com.github.cocosoys.mc.mcerp.service.SysNoticeService;
import com.github.cocosoys.mc.mcerp.service.SysUserService;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysExpansion;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MCERP 的 {@link SoysExpansion} 极简注册门户（随主插件 SoysExpansion 新书写同步）。
 * <ul>
 *   <li>构造收 {@link MCERP} 实例，内部集中创建 Service 层与全部 Controller 实例
 *       （本类自治，MCERP 主类无需暴露 service 字段）</li>
 *   <li>{@link #buildControllers()}：记录全部 controller 实例化（批量注册/注销共用同一来源）；
 *       主插件默认 {@code registerController()} 自动遍历本来源逐例正常登记
 *       （补 /plugins/&lt;插件名&gt; 前缀 → 路由 /api/plugins/MCERP/&lt;类级前缀&gt;/*），
 *       {@code unregisterController()} 自动逐例卸载——无需重写注册/注销逻辑</li>
 *   <li>{@link #resourceRoot()} 返回 "dist"：骨架 {@code registerPages()} 自动托管前端页面
 *       （等价原 {@code registerResourceDirectory(this,"/",cl,"dist")}，额外打
 *       {@code expansion:MCERP} tag → 可精确卸载）</li>
 *   <li>{@link #getIdentifier()}="MCERP"：页面 tag / 冲突检测 / 文档展示</li>
 * </ul>
 */
@Getter
public class McerpExpansion extends SoysExpansion {
    private final MCERP instance;

    // ===== Service 层 =====
    private final AuthService authService;
    private final MenuRouteService menuRouteService;
    private final OperLogService operLogService;
    private final SysUserService sysUserService;
    private final SysMenuService sysMenuService;
    private final SysDictService sysDictService;
    private final SysConfigService sysConfigService;
    private final SysNoticeService sysNoticeService;
    private final SysLogService sysLogService;

    public McerpExpansion(MCERP instance) {
        this.instance = instance;
        EripRegistry registry = instance.getRegistry();
        // ===== Service 层 =====
        authService = new AuthServiceImpl(registry);
        menuRouteService = new MenuRouteServiceImpl(registry, authService);
        operLogService = new OperLogServiceImpl();
        sysUserService = new SysUserServiceImpl(authService, operLogService);
        sysMenuService = new SysMenuServiceImpl(registry, operLogService);
        sysDictService = new SysDictServiceImpl(operLogService);
        sysConfigService = new SysConfigServiceImpl(operLogService);
        sysNoticeService = new SysNoticeServiceImpl(operLogService);
        sysLogService = new SysLogServiceImpl();

    }

    /** 模块唯一标识（页面 tag / 冲突检测 / 文档展示） */
    @Override
    public String getIdentifier() {
        return "MCERP";
    }

    /**
     * 记录所有 controller 实例化：主插件 registerController()/unregisterController()
     * 自动遍历本来源批量登记/注销（默认实现已内置，本扩展仅提供来源列表）。
     */
    @Override
    protected List<Object> buildControllers() {
        // ===== 记录所有 controller 实例化 =====
        List<Object> list = new ArrayList<>();
        list.add(new AuthController(authService, menuRouteService));
        list.add(new SysUserController(sysUserService));
        list.add(new SysMenuController(sysMenuService));
        list.add(new SysDictController(sysDictService));
        list.add(new SysConfigController(sysConfigService));
        list.add(new SysNoticeController(sysNoticeService));
        list.add(new SysLogController(sysLogService));
        /** 全部 Controller 实例清单（集中实例化，批量注册/注销共用） */
        return Collections.unmodifiableList(list);
    }

    /** 前端页面资源根：骨架 registerPages() 自动托管 dist 目录 */
    @Override
    protected String resourceRoot() {
        return "dist";
    }

}
