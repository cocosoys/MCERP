package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.entity.SysUser;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 登录链路服务（抽象契约）：登录/登出、当前主体解析、权限判定、登录模式探测。
 * 实现见 {@link AuthServiceImpl}；实现细节完全交给 SOYS 登录桥（AuthMe）。
 */
public interface AuthService {

    /**
     * 是否有登录插件提供者（AuthMe）：有 → 密码必检；无 → 免密码 + 验证码校验。
     */
    boolean isPasswordRequired();

    /**
     * 生成新图形验证码（无提供者时作为第二校验）。
     *
     * @return {uuid, img}(img 为 base64 PNG，无 data: 前缀，与若依契约一致)
     */
    Map<String, String> newCaptcha();

    AjaxResult login(String body);

    AjaxResult logout(CredentialPresentation credential);

    /**
     * 由 SOYS 网关解析后的凭证获取当前玩家名；null 表示未登录/未知。
     */
    String currentPlayer(CredentialPresentation credential);

    boolean hasPermission(CredentialPresentation credential, String permission);

    /**
     * 当前用户信息（若依 getInfo 契约）：user / roles / permissions。
     */
    AjaxResult getInfo(CredentialPresentation credential);

    SysUser findUser(String userName);

    boolean isOp(String player);

    /**
     * 玩家拥有的全部权限标识（内置 + 已登记模块），供前端菜单/按钮鉴权。
     */
    List<String> permissionsOf(CredentialPresentation credential);

    /**
     * 当前时间戳（若依 createTime 等契约格式）。
     */
    static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
