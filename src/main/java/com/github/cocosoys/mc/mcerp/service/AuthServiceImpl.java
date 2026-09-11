package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.EripMenu;
import com.github.cocosoys.mc.mcerp.EripModule;
import com.github.cocosoys.mc.mcerp.EripRegistry;
import com.github.cocosoys.mc.mcerp.entity.SysLogininfor;
import com.github.cocosoys.mc.mcerp.entity.SysUser;
import com.github.cocosoys.mc.mcerp.util.Json;
import com.github.cocosoys.mc.soyshttpovermc.HttpOverMcPlugin;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.bridge.spi.LoginProvider;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.IssuedCredential;
import lombok.CustomLog;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录链路实现（完全交给 SOYS 登录桥）：
 * <ol>
 *   <li>前端 POST {apiPrefix}/prod-api/login（RuoYi 契约，baseURL 运行时改写为 /api/prod-api）</li>
 *   <li>MCERP 用 SOYS 的 LoginProvider（AuthMe）校验密码；无提供者时免密码 + 验证码校验</li>
 *   <li>SOYS 会话令牌颁发器签发凭证，token 即 RuoYi 的 Authorization: Bearer</li>
 *   <li>MCERP 维护 token→玩家 映射（SOYS 重启后旧 token 失效，需重新登录，自洽）</li>
 * </ol>
 */
@CustomLog
public class AuthServiceImpl implements AuthService {

    private final EripRegistry registry;
    private final CaptchaService captchaService = new CaptchaServiceImpl();

    /** bearer token -> 玩家名 */
    private final Map<String, String> tokenSubjects = new ConcurrentHashMap<>();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public AuthServiceImpl(EripRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean isPasswordRequired() {
        HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
        LoginProvider provider = soys == null ? null : soys.getLoginProvider();
        return provider != null && provider.isAvailable();
    }

    @Override
    public Map<String, String> newCaptcha() {
        return captchaService.newCaptcha();
    }

    @Override
    public AjaxResult login(String body) {
        Map<String, Object> map = body == null || body.isEmpty()
                ? new LinkedHashMap<>() : Json.parseObject(body);
        String username = Json.getString(map, "username", "");
        String password = Json.getString(map, "password", "");
        String code = Json.getString(map, "code", "");
        String uuid = Json.getString(map, "uuid", "");
        if (username.isEmpty()) {
            return AjaxResult.error("用户名不能为空");
        }
        boolean pwdRequired = isPasswordRequired();
        if (!pwdRequired && !captchaService.verify(uuid, code)) {
            recordLogin(username, "1", "登录失败：验证码错误");
            return AjaxResult.error("验证码错误");
        }
        HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
        if (soys == null) {
            return AjaxResult.error("SOYSHTTPOverMC 未就绪");
        }
        LoginProvider provider = soys.getLoginProvider();
        if (pwdRequired) {
            if (password.isEmpty()) {
                return AjaxResult.error("请输入密码");
            }
            if (provider == null) {
                recordLogin(username, "1", "登录失败：SOYS 登录桥不可用（请安装 AuthMe 并配置登录桥）");
                return AjaxResult.error("登录服务不可用：未接入 AuthMe 登录桥");
            }
        }
        // 后台账号停用检查（不强制要求存在记录：AuthMe 验证通过即可登录）
        for (SysUser u : Store.select(SysUser.class)) {
            if (username.equalsIgnoreCase(u.getUserName())) {
                if ("1".equals(u.getStatus())) {
                    recordLogin(username, "1", "登录失败：账号已被停用");
                    return AjaxResult.error("账号已被停用，请联系管理员");
                }
                break;
            }
        }
        boolean ok = !pwdRequired;
        if (pwdRequired) {
            try {
                ok = provider.verifyPassword(username, password);
            } catch (Exception e) {
                ok = false;
            }
        }
        if (!ok) {
            recordLogin(username, "1", "登录失败：账号或密码错误");
            return AjaxResult.error("账号或密码错误");
        }
        IssuedCredential credential = soys.getApi().getAuthCredential().issueCredential(username);
        if (credential == null || credential.getBearer() == null) {
            recordLogin(username, "1", "登录失败：会话令牌颁发器未启用");
            return AjaxResult.error("会话令牌颁发器未启用，请联系管理员");
        }
        tokenSubjects.put(credential.getBearer(), username);
        recordLogin(username, "0", "登录成功");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", credential.getBearer());
        return AjaxResult.success(data);
    }

    @Override
    public AjaxResult logout(CredentialPresentation credential) {
        if (credential != null && credential.getBearer() != null) {
            tokenSubjects.remove(credential.getBearer());
        }
        return AjaxResult.success("退出成功");
    }

    @Override
    public String currentPlayer(CredentialPresentation credential) {
        if (credential == null || credential.getBearer() == null) {
            return null;
        }
        String subject = tokenSubjects.get(credential.getBearer());
        return subject == null ? credential.getBearer() : subject;
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
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", userInfo(player));
        List<String> roles = new ArrayList<>();
        roles.add(isOp(player) ? "admin" : "common");
        data.put("roles", roles);
        data.put("permissions", permissionsOf(credential));
        return AjaxResult.success(data);
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
        for (SysUser u : Store.select(SysUser.class)) {
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
    public List<String> permissionsOf(CredentialPresentation credential) {
        List<String> result = new ArrayList<>();
        for (String perm : BuiltinMenus.ALL_PERMS) {
            if (hasPermission(credential, perm)) {
                result.add(perm);
            }
        }
        for (EripModule m : registry.getModules()) {
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

    private void collectMenuPerms(List<EripMenu> menus,
                                  CredentialPresentation credential, List<String> out) {
        for (EripMenu m : menus) {
            collectPerms(m.getPerms(), credential, out);
            collectMenuPerms(m.getChildren(), credential, out);
        }
    }

    // ===== 登录日志 =====

    private void recordLogin(String userName, String status, String msg) {
        try {
            SysLogininfor info = new SysLogininfor();
            info.setInfoId(UUID.randomUUID().toString());
            info.setUserName(userName);
            info.setIpaddr("");
            info.setStatus(status);
            info.setMsg(msg);
            info.setLoginTime(SDF.format(new Date()));
            Store.insert(info);
        } catch (Exception e) {
            log.warn("登录日志写入失败: %s", e.getMessage());
        }
    }
}
