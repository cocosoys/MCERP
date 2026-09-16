package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.entity.SysUser;
import com.github.cocosoys.mc.soyshttpovermc.permission.local.LocalPermissionStore;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 用户信息服务（抽象契约）：登录会话完全交给 SOYS 主插件自带 auth（/api/auth/* + soys-auth.js），
 * 本服务仅提供当前主体解析、权限判定与若依 getInfo 契约。实现见 {@link AuthServiceImpl}。
 */
public interface AuthService {

    /**
     * 由 SOYS 网关解析后的凭证获取当前玩家名（CombinedPermissionService.subjectOf）；
     * null 表示未登录/未知。
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
     * SOYS 本地权限存储（权限组/用户权限节点读写，OP 默认拥有全部权限）。
     */
    LocalPermissionStore getPermissionStore();

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
