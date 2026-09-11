package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.MenuRouteService;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPublic;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 若依登录链路：经 MCERP#initSoys 的 registerProxyController 代理注册（无 /plugins/MCERP 前缀），
 * SOYS 注册时自动补全局前缀 /api → 实际路由 /api/prod-api/*（匹配 RuoYi-Vue axios baseURL）：
 * captchaImage / login / logout / getInfo / getRouters。
 * 鉴权完全交给 SOYS 登录桥（AuthMe）与会话令牌。
 */
@RequestMapping("/prod-api")
public class AuthController {

    private final AuthService authService;
    private final MenuRouteService routeService;

    public AuthController(AuthService authService, MenuRouteService routeService) {
        this.authService = authService;
        this.routeService = routeService;
    }

    @ApiName("获取验证码状态")
    @ApiPublic
    @GetMapping("/captchaImage")
    public AjaxResult captchaImage() {
        Map<String, Object> data = new LinkedHashMap<>();
        // 无登录插件提供者（AuthMe）时开启验证码作为第二校验；有提供者则密码校验，关闭验证码
        boolean enabled = !authService.isPasswordRequired();
        data.put("captchaEnabled", enabled);
        if (enabled) {
            Map<String, String> cap = authService.newCaptcha();
            data.put("uuid", cap.get("uuid"));
            data.put("img", cap.get("img"));
        } else {
            data.put("uuid", "");
            data.put("img", "");
        }
        return AjaxResult.success(data);
    }

    @ApiName("若依登录")
    @ApiPublic
    @PostMapping("/login")
    public AjaxResult login(@RequestBody String body) {
        return authService.login(body);
    }

    @ApiName("若依退出")
    @ApiPublic
    @PostMapping("/logout")
    public AjaxResult logout(CredentialPresentation credential) {
        return authService.logout(credential);
    }

    @ApiName("当前用户信息")
    @GetMapping("/getInfo")
    public AjaxResult getInfo(CredentialPresentation credential) {
        return authService.getInfo(credential);
    }

    @ApiName("路由菜单")
    @GetMapping("/getRouters")
    public AjaxResult getRouters(CredentialPresentation credential) {
        return AjaxResult.success(routeService.buildRoutes(credential));
    }
}
