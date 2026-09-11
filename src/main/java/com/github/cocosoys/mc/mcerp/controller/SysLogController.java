package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.SysLogininfor;
import com.github.cocosoys.mc.mcerp.entity.SysOperLog;
import com.github.cocosoys.mc.mcerp.service.Store;
import com.github.cocosoys.mc.mcerp.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.DeleteMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 日志管理（若依契约：经 registerProxyController 代理注册，SOYS 自动补 /api 全局前缀
 * → 实际路由 /api/prod-api/monitor/operlog + /api/prod-api/monitor/logininfor）。
 */
@RequestMapping("/prod-api/monitor")
public class SysLogController {

    public SysLogController() {
    }

    // ===== 操作日志 =====

    @ApiName("操作日志列表")
    @ApiPermission("monitor:operlog:list")
    @GetMapping("/operlog/list")
    public TableDataInfo operlogList(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                                     @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                     @RequestParam(name = "title", required = false) String title,
                                     @RequestParam(name = "operName", required = false) String operName) {
        List<SysOperLog> all = Store.select(SysOperLog.class);
        List<SysOperLog> filtered = new ArrayList<>();
        for (SysOperLog l : all) {
            if (title != null && !title.isEmpty() && !contains(l.getTitle(), title)) {
                continue;
            }
            if (operName != null && !operName.isEmpty() && !contains(l.getOperName(), operName)) {
                continue;
            }
            filtered.add(l);
        }
        filtered.sort(Comparator.comparing(SysOperLog::getOperTime, Comparator.nullsLast(String::compareTo)).reversed());
        return SysUserController.page(filtered, pageNum, pageSize);
    }

    @ApiName("删除操作日志")
    @ApiPermission("monitor:operlog:remove")
    @DeleteMapping("/operlog/{operIds}")
    public AjaxResult operlogRemove(@PathVariable(name = "operIds") String operIds) {
        if (operIds == null || operIds.isEmpty()) {
            return AjaxResult.error("缺少 operId");
        }
        for (String id : operIds.split(",")) {
            if (!id.trim().isEmpty()) {
                Store.deleteById(SysOperLog.class, id.trim());
            }
        }
        return AjaxResult.success("删除成功");
    }

    @ApiName("清空操作日志")
    @ApiPermission("monitor:operlog:clean")
    @DeleteMapping("/operlog/clean")
    public AjaxResult operlogClean() {
        for (SysOperLog l : Store.select(SysOperLog.class)) {
            Store.deleteById(SysOperLog.class, l.getOperId());
        }
        return AjaxResult.success("清空成功");
    }

    // ===== 登录日志 =====

    @ApiName("登录日志列表")
    @ApiPermission("monitor:logininfor:list")
    @GetMapping("/logininfor/list")
    public TableDataInfo logininforList(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                                        @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                        @RequestParam(name = "userName", required = false) String userName,
                                        @RequestParam(name = "status", required = false) String status) {
        List<SysLogininfor> all = Store.select(SysLogininfor.class);
        List<SysLogininfor> filtered = new ArrayList<>();
        for (SysLogininfor l : all) {
            if (userName != null && !userName.isEmpty() && !contains(l.getUserName(), userName)) {
                continue;
            }
            if (status != null && !status.isEmpty() && !status.equals(l.getStatus())) {
                continue;
            }
            filtered.add(l);
        }
        filtered.sort(Comparator.comparing(SysLogininfor::getLoginTime, Comparator.nullsLast(String::compareTo)).reversed());
        return SysUserController.page(filtered, pageNum, pageSize);
    }

    @ApiName("删除登录日志")
    @ApiPermission("monitor:logininfor:remove")
    @DeleteMapping("/logininfor/{infoIds}")
    public AjaxResult logininforRemove(@PathVariable(name = "infoIds") String infoIds) {
        if (infoIds == null || infoIds.isEmpty()) {
            return AjaxResult.error("缺少 infoId");
        }
        for (String id : infoIds.split(",")) {
            if (!id.trim().isEmpty()) {
                Store.deleteById(SysLogininfor.class, id.trim());
            }
        }
        return AjaxResult.success("删除成功");
    }

    @ApiName("清空登录日志")
    @ApiPermission("monitor:logininfor:clean")
    @DeleteMapping("/logininfor/clean")
    public AjaxResult logininforClean() {
        for (SysLogininfor l : Store.select(SysLogininfor.class)) {
            Store.deleteById(SysLogininfor.class, l.getInfoId());
        }
        return AjaxResult.success("清空成功");
    }

    @ApiName("解锁账号")
    @ApiPermission("monitor:logininfor:unlock")
    @GetMapping("/logininfor/unlock/{userName}")
    public AjaxResult unlock(@PathVariable(name = "userName") String userName) {
        return AjaxResult.success("解锁成功（由 AuthMe 管理）");
    }

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
