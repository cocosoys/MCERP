package com.github.cocosoys.mc.mcerp.service;

/**
 * 操作日志横切服务（抽象契约）：各业务模块写操作后统一记录 erp_oper_log。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.OperLogServiceImpl}。
 */
public interface OperLogService {

    /**
     * 记录一条操作日志（写入失败静默吞异常，不影响主业务）。
     *
     * @param title    模块标题（如 用户管理/菜单管理）
     * @param business 业务动作（如 新增用户/修改用户/删除用户）
     * @param operName 操作对象名
     * @param result   结果（如 新增成功/修改成功）
     */
    void record(String title, String business, String operName, String result);
}
