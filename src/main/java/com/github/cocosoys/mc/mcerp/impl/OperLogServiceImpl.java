package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.entity.ErpOperLog;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import lombok.CustomLog;

/**
 * 操作日志实现（从 ErpUserController.recordOper 剥离）：写入 erp_oper_log。
 * <p>操作者经 {@link AuthService#currentPlayer} 由 SOYS 凭证解析（真实玩家名）；
 * 操作对象名写入 operParam；businessType 按业务动作文本推导（新增/修改/删除/其它）。
 * 写入失败打 warn 日志——日志横切不阻断主业务，但失败可观测（不再静默吞异常）。
 */
@CustomLog
public class OperLogServiceImpl implements OperLogService {

    private final AuthService auth;

    public OperLogServiceImpl(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void record(CredentialPresentation credential, String title, String business, String target, String result) {
        try {
            ErpOperLog log = new ErpOperLog();
            log.setTitle(title);
            String b = business == null ? "" : business;
            log.setBusinessType(b.contains("新增") ? 1 : b.contains("修改") ? 2 : b.contains("删除") ? 3 : 0);
            log.setMethod(business);
            log.setRequestMethod("");
            String operator = auth == null ? null : auth.currentPlayer(credential);
            log.setOperName(operator == null ? "" : operator);
            log.setOperUrl("");
            log.setOperIp("");
            log.setOperParam(target == null ? "" : target);
            log.setJsonResult("");
            log.setStatus(0);
            log.setErrorMsg("");
            log.setOperTime(AuthService.now());
            DATA.insert(log);
        } catch (Exception e) {
            log.warnT("mcerp.operlog.write-failed", "操作日志写入失败: {0}", e.toString());
        }
    }
}
