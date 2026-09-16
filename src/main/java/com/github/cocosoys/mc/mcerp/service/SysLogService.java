package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 日志管理服务（抽象契约）：操作日志/登录日志列表、删除、清空。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.SysLogServiceImpl}。
 */
public interface SysLogService {

    TableDataInfo operlogList(Integer pageNum, Integer pageSize, String title, String operName);

    AjaxResult operlogRemove(String operIds);

    AjaxResult operlogClean();

    TableDataInfo logininforList(Integer pageNum, Integer pageSize, String userName, String status);

    AjaxResult logininforRemove(String infoIds);

    AjaxResult logininforClean();

    AjaxResult unlock(String userName);
}
