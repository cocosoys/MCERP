package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.controller.ErpAuthController;
import com.github.cocosoys.mc.mcerp.controller.ErpConfigController;
import com.github.cocosoys.mc.mcerp.controller.ErpDictController;
import com.github.cocosoys.mc.mcerp.controller.ErpLogController;
import com.github.cocosoys.mc.mcerp.controller.ErpMenuController;
import com.github.cocosoys.mc.mcerp.controller.ErpNoticeController;
import com.github.cocosoys.mc.mcerp.controller.ErpUserController;
import com.github.cocosoys.mc.mcerp.impl.AuthServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.MenuRouteServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.OperLogServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.ErpConfigServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.ErpDictServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.ErpLogServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.ErpMenuServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.ErpNoticeServiceImpl;
import com.github.cocosoys.mc.mcerp.impl.ErpUserServiceImpl;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.MenuRouteService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.ErpConfigService;
import com.github.cocosoys.mc.mcerp.service.ErpDictService;
import com.github.cocosoys.mc.mcerp.service.ErpLogService;
import com.github.cocosoys.mc.mcerp.service.ErpMenuService;
import com.github.cocosoys.mc.mcerp.service.ErpNoticeService;
import com.github.cocosoys.mc.mcerp.service.ErpUserService;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysExpansion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MCERP 宿主扩展（原 McerpExpansion 更名承接）：MCERP 自身的 {@link SoysExpansion} 门户。
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
 *
 * <p>宿主不参与 ERP 模块登记：本类是 SoysExpansion 直系子类（非 {@link McerpExpansion}），
 * MCERP 的"是否 ERP 模块"判定（{@code instanceof McerpExpansion}）天然排除宿主。</p>
 */
public class McErpHostExpansion extends SoysExpansion {

    /** 全部 Controller 实例清单（集中实例化，批量注册/注销共用） */
    private final List<Object> controllers;

    public McErpHostExpansion(MCERP instance) {
        ErpRegistry registry = instance.getRegistry();
        // ===== Service 层 =====
        AuthService authService = new AuthServiceImpl(registry);
        MenuRouteService menuRouteService = new MenuRouteServiceImpl(registry, authService);
        OperLogService operLogService = new OperLogServiceImpl();
        ErpUserService sysUserService = new ErpUserServiceImpl(authService, operLogService);
        ErpMenuService sysMenuService = new ErpMenuServiceImpl(registry, operLogService);
        ErpDictService sysDictService = new ErpDictServiceImpl(operLogService);
        ErpConfigService sysConfigService = new ErpConfigServiceImpl(operLogService);
        ErpNoticeService sysNoticeService = new ErpNoticeServiceImpl(operLogService);
        ErpLogService sysLogService = new ErpLogServiceImpl();
        // ===== 记录所有 controller 实例化 =====
        List<Object> list = new ArrayList<>();
        list.add(new ErpAuthController(authService, menuRouteService));
        list.add(new ErpUserController(sysUserService));
        list.add(new ErpMenuController(sysMenuService));
        list.add(new ErpDictController(sysDictService));
        list.add(new ErpConfigController(sysConfigService));
        list.add(new ErpNoticeController(sysNoticeService));
        list.add(new ErpLogController(sysLogService));
        this.controllers = list;
    }

    /** 模块唯一标识（页面 tag / 冲突检测 / 文档展示） */
    @Override
    public String getIdentifier() {
        return MCERP.getInstance().getName();
    }

    /**
     * 记录所有 controller 实例化：主插件 registerController()/unregisterController()
     * 自动遍历本来源批量登记/注销（默认实现已内置，本扩展仅提供来源列表）。
     */
    @Override
    protected List<Object> buildControllers() {
        return Collections.unmodifiableList(controllers);
    }

    /** 前端页面资源根：骨架 registerPages() 自动托管 dist 目录 */
    @Override
    protected String resourceRoot() {
        return "dist";
    }

    @Override protected String[] dataRoots() {
        return new String[]{"data"};
    }

    @Override protected String[] sqlRoots() {
        return new String[]{"sql"};
    }
}
