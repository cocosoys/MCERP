package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.entity.ErpLogininfor;
import com.github.cocosoys.mc.mcerp.entity.ErpOperLog;
import com.github.cocosoys.mc.mcerp.service.ErpLogService;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 日志管理实现（从 ErpLogController 迁入）：操作/登录日志列表、删除、清空。
 */
public class ErpLogServiceImpl implements ErpLogService {

    // ===== 操作日志 =====

    @Override
    public TableDataInfo operlogList(Integer pageNum, Integer pageSize, String title, String operName) {
        List<ErpOperLog> all = DATA.select(ErpOperLog.class);
        List<ErpOperLog> filtered = new ArrayList<>();
        for (ErpOperLog l : all) {
            if (title != null && !title.isEmpty() && !contains(l.getTitle(), title)) {
                continue;
            }
            if (operName != null && !operName.isEmpty() && !contains(l.getOperName(), operName)) {
                continue;
            }
            filtered.add(l);
        }
        filtered.sort(Comparator.comparing(ErpOperLog::getOperTime, Comparator.nullsLast(Date::compareTo)).reversed());
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult operlogRemove(String operIds) {
        if (operIds == null || operIds.isEmpty()) {
            return AjaxResult.error("缺少 operId");
        }
        for (String id : operIds.split(",")) {
            if (!id.trim().isEmpty()) {
                DATA.deleteById(ErpOperLog.class, id.trim());
            }
        }
        return AjaxResult.success("删除成功");
    }

    @Override
    public AjaxResult operlogClean() {
        for (ErpOperLog l : DATA.select(ErpOperLog.class)) {
            DATA.deleteById(ErpOperLog.class, l.getOperId());
        }
        return AjaxResult.success("清空成功");
    }

    // ===== 登录日志 =====

    @Override
    public TableDataInfo logininforList(Integer pageNum, Integer pageSize, String userName, String status) {
        List<ErpLogininfor> all = DATA.select(ErpLogininfor.class);
        List<ErpLogininfor> filtered = new ArrayList<>();
        for (ErpLogininfor l : all) {
            if (userName != null && !userName.isEmpty() && !contains(l.getUserName(), userName)) {
                continue;
            }
            if (status != null && !status.isEmpty() && !status.equals(l.getStatus())) {
                continue;
            }
            filtered.add(l);
        }
        filtered.sort(Comparator.comparing(ErpLogininfor::getLoginTime, Comparator.nullsLast(Date::compareTo)).reversed());
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult logininforRemove(String infoIds) {
        if (infoIds == null || infoIds.isEmpty()) {
            return AjaxResult.error("缺少 infoId");
        }
        for (String id : infoIds.split(",")) {
            if (!id.trim().isEmpty()) {
                DATA.deleteById(ErpLogininfor.class, id.trim());
            }
        }
        return AjaxResult.success("删除成功");
    }

    @Override
    public AjaxResult logininforClean() {
        for (ErpLogininfor l : DATA.select(ErpLogininfor.class)) {
            DATA.deleteById(ErpLogininfor.class, l.getInfoId());
        }
        return AjaxResult.success("清空成功");
    }

    @Override
    public AjaxResult unlock(String userName) {
        return AjaxResult.success("解锁成功（由 AuthMe 管理）");
    }

    // ===== 内部 =====

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
