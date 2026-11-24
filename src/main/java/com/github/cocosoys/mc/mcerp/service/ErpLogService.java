package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 日志管理服务（抽象契约）：操作日志/登录日志列表、导出、删除、清空。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.ErpLogServiceImpl}。
 */
public interface ErpLogService {

    TableDataInfo operlogList(Integer pageNum, Integer pageSize, String title, String operName);

    /**
     * 导出操作日志为 CSV：筛选口径与 {@link #operlogList} 一致（同 title/operName 条件、同时间倒序）。
     *
     * @param title    模块标题模糊条件（可空）
     * @param operName 操作人员模糊条件（可空）
     * @return {@code AjaxResult.success(LogExportVO)}：文件名 + CSV 全文 + 行数
     */
    AjaxResult operlogExport(String title, String operName);

    AjaxResult operlogRemove(String operIds);

    AjaxResult operlogClean();

    TableDataInfo logininforList(Integer pageNum, Integer pageSize, String userName, String status);

    /**
     * 导出登录日志为 CSV：筛选口径与 {@link #logininforList} 一致（同 userName/status 条件、同时间倒序）。
     *
     * @param userName 用户账号模糊条件（可空）
     * @param status   登录状态精确条件（0 成功 / 1 失败，可空）
     * @return {@code AjaxResult.success(LogExportVO)}：文件名 + CSV 全文 + 行数
     */
    AjaxResult logininforExport(String userName, String status);

    AjaxResult logininforRemove(String infoIds);

    AjaxResult logininforClean();

    AjaxResult unlock(String userName);
}
