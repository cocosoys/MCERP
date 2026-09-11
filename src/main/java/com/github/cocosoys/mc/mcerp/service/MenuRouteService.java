package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.List;
import java.util.Map;

/**
 * getRouters 路由合成服务（抽象契约）：内置菜单 ⊕ 已登记 ERP 模块菜单。
 * 实现见 {@link MenuRouteServiceImpl}。
 */
public interface MenuRouteService {

    /**
     * 合成当前凭证可见的路由树（若依 getRouters 契约）。
     */
    List<Map<String, Object>> buildRoutes(CredentialPresentation credential);
}
