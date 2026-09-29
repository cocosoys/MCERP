package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.entity.ErpOperLog;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import lombok.CustomLog;

/**
 * 操作日志实现（从 ErpUserController.recordOper 剥离）：写入 erp_oper_log。
 * <p>操作者经 {@link AuthService#currentPlayer} 由 SOYS 凭证解析（真实玩家名）；
 * 操作对象名写入 operParam；businessType 按业务动作文本推导（新增/修改/删除/其它）。
 * 请求原信息（请求方法/请求地址/客户端 IP）经主插件 {@link ApiRequestContext#current()}
 * ThreadLocal 免签名改动读取（网关派发时绑定到当前 worker 线程）；返回结果文案写入 jsonResult。
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
            // 当前请求上下文（网关派发时绑定；非请求线程/内部调用为 null）
            ApiRequestContext ctx = ApiRequestContext.current();
            ErpOperLog log = new ErpOperLog();
            log.setTitle(title);
            String b = business == null ? "" : business;
            log.setBusinessType(b.contains("新增") ? 1 : b.contains("修改") ? 2 : b.contains("删除") ? 3 : 0);
            log.setMethod(business);
            log.setRequestMethod(ctx == null ? "" : ctx.getHttpMethod());
            String operator = auth == null ? null : auth.currentPlayer(credential);
            log.setOperName(operator == null ? "" : operator);
            log.setOperUrl(ctx == null ? "" : ctx.getPath());
            log.setOperIp(ctx == null ? "" : ctx.getIp());
            log.setOperParam(target == null ? "" : target);
            log.setJsonResult(result == null ? "" : result);
            log.setStatus(0);
            log.setErrorMsg("");
            log.setOperTime(AuthService.now());
            DATA.insert(log);
        } catch (Exception e) {
            log.warnT("mcerp.operlog.write-failed", "操作日志写入失败: {0}", e.toString());
        }
    }
}
