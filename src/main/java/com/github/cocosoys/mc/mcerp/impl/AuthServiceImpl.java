package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.EripRegistry;
import com.github.cocosoys.mc.mcerp.entity.SysMenu;
import com.github.cocosoys.mc.mcerp.entity.SysUser;
import com.github.cocosoys.mc.mcerp.entity.vo.EripMenuVO;
import com.github.cocosoys.mc.mcerp.entity.vo.EripModuleVO;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.soyshttpovermc.HttpOverMcPlugin;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.permission.local.LocalPermissionStore;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户信息实现：登录会话完全交给 SOYS 主插件 auth（/api/auth/* + soys-auth.js），
 * 本实现只做：
 * <ul>
 *   <li>currentPlayer：经 SOYS CombinedPermissionService.subjectOf 解析 Bearer/Cookie 凭证主体；</li>
 *   <li>hasPermission / isOp / permissionsOf：SOYS 组合权限判定（OP 全放行）；</li>
 *   <li>getInfo：若依契约 {user, roles, permissions}。</li>
 * </ul>
 */
public class AuthServiceImpl implements AuthService {

    private final EripRegistry registry;

    public AuthServiceImpl(EripRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String currentPlayer(CredentialPresentation credential) {
        if (credential == null || !credential.hasAnyCredential()) {
            return null;
        }
        HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
        if (soys == null || soys.getCombinedPermissionService() == null) {
            return null;
        }
        String player = soys.getCombinedPermissionService().subjectOf(credential);
        if (player != null && !player.isEmpty()) {
            ensureUserRegistered(player);
        }
        return player;
    }

    /** 首次进入 ERP 自动登记到 erp_user（便于用户管理页做按钮权限分配）。 */
    private final Set<String> registeredUsers = new HashSet<>();

    private void ensureUserRegistered(String player) {
        if (!registeredUsers.add(player.toLowerCase())) {
            return; // 本进程已登记过
        }
        for (SysUser u : DATA.select(SysUser.class)) {
            if (player.equalsIgnoreCase(u.getUserName())) {
                return; // 已存在
            }
        }
        SysUser u = new SysUser();
        u.setUserId(player);
        u.setUserName(player);
        u.setNickName(player);
        u.setEmail("");
        u.setPhonenumber("");
        u.setSex("0");
        u.setStatus("0");
        u.setRemark("首次进入 ERP 自动登记");
        u.setCreateTime(AuthService.now());
        DATA.insert(u);
    }

    @Override
    public boolean hasPermission(CredentialPresentation credential, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
        if (soys == null || soys.getCombinedPermissionService() == null) {
            return true; // SOYS 未就绪时不阻断
        }
        return soys.getCombinedPermissionService().hasPermission(credential, permission);
    }

    @Override
    public AjaxResult getInfo(CredentialPresentation credential) {
        String player = currentPlayer(credential);
        if (player == null) {
            return AjaxResult.error("未登录");
        }
        // RuoYi 契约：user/roles/permissions 放顶层，前端 store/modules/user.js 直接读 res.user/res.roles/res.permissions
        AjaxResult ok = AjaxResult.success();
        ok.put("user", userInfo(player));
        List<String> roles = new ArrayList<>();
        roles.add(isOp(player) ? "admin" : "common");
        ok.put("roles", roles);
        ok.put("permissions", permissionsOf(credential));
        return ok;
    }

    private Map<String, Object> userInfo(String player) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("userId", player);
        user.put("userName", player);
        user.put("nickName", player);
        user.put("avatar", "");
        SysUser u = findUser(player);
        user.put("sex", u == null ? "0" : u.getSex());
        user.put("email", u == null ? "" : u.getEmail());
        user.put("phonenumber", u == null ? "" : u.getPhonenumber());
        user.put("status", u == null ? "0" : u.getStatus());
        return user;
    }

    @Override
    public SysUser findUser(String userName) {
        for (SysUser u : DATA.select(SysUser.class)) {
            if (userName != null && userName.equalsIgnoreCase(u.getUserName())) {
                return u;
            }
        }
        return null;
    }

    @Override
    public boolean isOp(String player) {
        if (player == null) {
            return false;
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(player);
        return op != null && op.isOp();
    }

    @Override
    public LocalPermissionStore getPermissionStore() {
        HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
        if (soys == null || soys.getCombinedPermissionService() == null) {
            return null;
        }
        return soys.getCombinedPermissionService().getLocalStore();
    }

    @Override
    public List<String> permissionsOf(CredentialPresentation credential) {
        List<String> result = new ArrayList<>();
        // 内置 + 自定义菜单权限（erp_menu 表驱动，含初始化数据中的按钮 F 权限标识）
        for (SysMenu m : DATA.select(SysMenu.class)) {
            String perm = m.getPerms();
            if (perm != null && !perm.isEmpty() && !result.contains(perm) && hasPermission(credential, perm)) {
                result.add(perm);
            }
        }
        for (EripModuleVO m : registry.getModules()) {
            collectPerms(m.getPermission(), credential, result);
            collectMenuPerms(m.getChildren(), credential, result);
        }
        return result;
    }

    private void collectPerms(String perm, CredentialPresentation credential, List<String> out) {
        if (perm != null && !perm.isEmpty() && !out.contains(perm) && hasPermission(credential, perm)) {
            out.add(perm);
        }
    }

    private void collectMenuPerms(List<EripMenuVO> menus,
                                  CredentialPresentation credential, List<String> out) {
        for (EripMenuVO m : menus) {
            collectPerms(m.getPerms(), credential, out);
            collectMenuPerms(m.getChildren(), credential, out);
        }
    }
}
