package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.MenuRouteService;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPublic;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 若依用户信息链路：路由 /api/plugins/MCERP/auth/*（经 MCERP#initSoys 的 registerController 正常登记）。
 *
 * <p><b>auth 策略</b>：登录/登出/验证码/会话完全交给 SOYS 主插件自带 auth
 * （/api/auth/login|logout|me|status|mode + 前端 soys-auth.js），本控制器不再自行实现登录链路；
 * 仅保留若依前端契约的两个只读端点：
 * <ul>
 *   <li>getInfo：当前登录者信息（user/roles/permissions，token 主体经 SOYS 凭证解析）；</li>
 *   <li>getRouters：当前登录者可见路由树（菜单表 ⊕ 插件登记模块）。</li>
 * </ul>
 */
@RequestMapping("/auth")
/**
 * 认证控制器：登录/登出/验证码/会话信息（委托 SOYS 主插件 auth）。
 */
public class ErpAuthController {

    private final AuthService authService;
    private final MenuRouteService routeService;

    public ErpAuthController(AuthService authService, MenuRouteService routeService) {
        this.authService = authService;
        this.routeService = routeService;
    }

    @ApiName("当前用户信息")
    @ApiPublic // 网关 auth 策略已保护 /api/*（无凭证 401），此处仅解除注册表默认拒绝 → 语义为「已登录即可」
    @GetMapping("/getInfo")
    public AjaxResult getInfo(CredentialPresentation credential) {
        return authService.getInfo(credential);
    }

    @ApiName("路由菜单")
    @ApiPublic // 同上：网关 401 兜底，登录即可获取本人路由
    @GetMapping("/getRouters")
    public AjaxResult getRouters(CredentialPresentation credential) {
        return AjaxResult.success(routeService.buildRoutes(credential));
    }
}
