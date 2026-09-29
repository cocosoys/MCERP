package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 操作日志横切服务（抽象契约）：各业务模块写操作后统一记录 erp_oper_log。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.OperLogServiceImpl}。
 */
public interface OperLogService {

    /**
     * 记录一条操作日志（写入失败打 warn 日志，不影响主业务）。
     *
     * @param credential 当前请求凭证（SOYS 网关注入，经 {@link AuthService#currentPlayer} 解析操作者）
     * @param title      模块标题（如 用户管理/菜单管理）
     * @param business   业务动作（如 新增用户/修改用户/删除用户；用于推导 businessType）
     * @param target     操作对象名（如 configKey/userName/menuName，写入 operParam）
     * @param result     结果（如 新增成功/修改成功）
     */
    void record(CredentialPresentation credential, String title, String business, String target, String result);
}
