package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.soyshttpovermc.api.SoysExpansion;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysHttpOverMcApi;
import lombok.CustomLog;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MCERP 的 {@link SoysExpansion} 极简注册门户。
 * <ul>
 *   <li>{@link #registerController()} 覆写：批量正常登记全部 Controller 实例
 *       （自动补 /plugins/&lt;插件名&gt; 前缀 → 路由 /api/plugins/MCERP/&lt;类级前缀&gt;/*）</li>
 *   <li>{@link #controllers()}：集中记录所有 controller 实例化（批量注册/注销共用同一来源）</li>
 *   <li>{@link #resourceRoot()} 返回 "dist"：骨架 {@code registerPages()} 自动托管前端页面
 *       （等价原 {@code registerResourceDirectory(this,"/",cl,"dist")}，额外打
 *       {@code expansion:MCERP} tag → 可精确卸载）</li>
 *   <li>{@link #unregisterControllers()} 配套覆写：默认只卸载 this 自身，批量登记的
 *       独立实例须按 owner 插件名整组卸载（{@code unregisterPluginControllers}）</li>
 * </ul>
 */
@CustomLog
public class McerpExpansion extends SoysExpansion {

    private final SoysHttpOverMcApi soysApi;

    /** 全部 Controller 实例清单（集中实例化，批量注册/注销共用） */
    private final List<Object> controllers;

    public McerpExpansion(SoysHttpOverMcApi soysApi, List<Object> controllers) {
        this.soysApi = soysApi;
        this.controllers = controllers == null ? new ArrayList<>() : new ArrayList<>(controllers);
    }

    /** 模块唯一标识（页面 tag / 冲突检测 / 文档展示） */
    @Override
    public String getIdentifier() {
        return "MCERP";
    }

    /**
     * 记录所有 controller 实例化：返回只读清单，供批量注册与注销使用。
     */
    public List<Object> controllers() {
        return Collections.unmodifiableList(controllers);
    }

    /** 前端页面资源根：骨架 registerPages() 自动托管 dist 目录 */
    @Override
    protected String resourceRoot() {
        return "dist";
    }

    /**
     * 重写 registerController()：批量正常登记全部 Controller 实例。
     * <p>默认实现登记本扩展类自身；本扩展的端点书写在独立 Controller 类中，
     * 故逐实例登记（{@code force=false}，重复路由仍由 SOYS 阻止）。</p>
     */
    @Override
    protected boolean registerController() {
        Plugin o = getOwner();
        if (soysApi == null || o == null) {
            log.warn("MCERP SoysExpansion 端点注册失败：soysApi 或 owner 未就绪");
            return false;
        }
        try {
            for (Object c : controllers) {
                soysApi.getApiRegistration().registerController(c, o, false);
            }
            return true;
        } catch (Exception ex) {
            log.warn("MCERP SoysExpansion 批量端点注册失败: {0}", ex.getMessage());
            return false;
        }
    }

    /**
     * 配套注销：默认 unregisterControllers() 仅卸载 this，批量登记的独立实例
     * 按 owner 插件名整组卸载（MCERP 名下全部端点）。
     */
    @Override
    protected void unregisterControllers() {
        Plugin o = getOwner();
        if (soysApi == null || o == null) {
            return;
        }
        try {
            soysApi.getApiRegistration().unregisterPluginControllers(o.getName());
        } catch (Exception ex) {
            log.warn("MCERP SoysExpansion 批量端点反注册失败: {0}", ex.getMessage());
        }
    }
}
