package com.github.cocosoys.mc.mcerp.listener;

import static com.github.cocosoys.mc.mcerp.i18n.McerpI18n.t;

import com.github.cocosoys.mc.mcerp.entity.ErpLogininfor;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.soyshttpovermc.api.event.GatewayEvent;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import lombok.CustomLog;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 登录日志监听器：挂 SOYS 网关事件写入 erp_logininfor。
 * <ul>
 *   <li>{@link GatewayEvent.GatewayLoginResultEvent}：网页/自动登录链路成败（POST /auth/login、记住我/游戏 IP 自动登录）→ 成功 status=0 / 失败 status=1</li>
 *   <li>{@link GatewayEvent.GatewayCredentialIssuedEvent}：登录成功（SOYS 向玩家签发凭证，AuthMe 桥登录/令牌换取）→ status=0</li>
 *   <li>{@link GatewayEvent.GatewayAccessDeniedEvent}：仅记录登录链路（path 含 /auth/）的拒绝 → status=1，记录原因与 IP</li>
 * </ul>
 * <p>登录完全委托主插件 SOYS auth，本监听器只做旁路记账，写入失败打 warn 不影响网关。</p>
 */
@CustomLog
public class LoginLogListener implements Listener {

    /** 登录结果（网页弹窗登录 / 记住我 / 游戏 IP 自动登录）：成功 status=0、失败 status=1，均带玩家/IP/原因。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginResult(GatewayEvent.GatewayLoginResultEvent event) {
        try {
            ErpLogininfor log = new ErpLogininfor();
            log.setUserName(event.getPlayer() == null ? "" : event.getPlayer());
            log.setIpaddr(event.getIp() == null ? "" : event.getIp());
            log.setStatus(event.isSuccess() ? "0" : "1");
            String reason = event.getReason() == null ? "" : event.getReason();
            log.setMsg(event.isSuccess()
                    ? t("mcerp.logininfor.success", "登录成功")
                    : t("mcerp.logininfor.failed", "登录失败") + ": " + reason);
            log.setLoginTime(AuthService.now());
            DATA.insert(log);
        } catch (Exception e) {
            log.warnT("mcerp.logininfor.write-failed", "登录日志写入失败: {0}", e.toString());
        }
    }

    /** 登录成功：SOYS 向玩家签发凭证 → status=0。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCredentialIssued(GatewayEvent.GatewayCredentialIssuedEvent event) {
        try {
            ErpLogininfor log = new ErpLogininfor();
            log.setUserName(event.getSubject() == null ? "" : event.getSubject());
            log.setIpaddr("");
            log.setStatus("0");
            String issuer = event.getIssuerName() == null ? "" : event.getIssuerName();
            log.setMsg(t("mcerp.logininfor.success", "登录成功") + (issuer.isEmpty() ? "" : " (" + issuer + ")"));
            log.setLoginTime(AuthService.now());
            DATA.insert(log);
        } catch (Exception e) {
            log.warnT("mcerp.logininfor.write-failed", "登录日志写入失败: {0}", e.toString());
        }
    }

    /** 登录失败：网关拒绝登录链路（/auth/*）→ status=1，记录原因与 IP。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAccessDenied(GatewayEvent.GatewayAccessDeniedEvent event) {
        String path = event.getPath();
        if (path == null || !path.contains("/auth/")) {
            return; // 仅记录登录链路，其余 401/403 不污染登录日志
        }
        try {
            ErpLogininfor log = new ErpLogininfor();
            log.setUserName("");
            log.setIpaddr(event.getIp() == null ? "" : event.getIp());
            log.setStatus("1");
            String reason = event.getReason() == null ? "" : event.getReason();
            log.setMsg(t("mcerp.logininfor.failed", "登录失败") + ": " + reason + " [" + event.getMethod() + " " + path + "]");
            log.setLoginTime(AuthService.now());
            DATA.insert(log);
        } catch (Exception e) {
            log.warnT("mcerp.logininfor.write-failed", "登录日志写入失败: {0}", e.toString());
        }
    }
}
