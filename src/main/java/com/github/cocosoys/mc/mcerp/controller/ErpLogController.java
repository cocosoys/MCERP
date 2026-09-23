package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.service.ErpLogService;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.DeleteMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 日志管理（若依契约）：路由 /api/plugins/MCERP/monitor/operlog + monitor/logininfor。
 * 列表/删除/清空全部委托 {@link ErpLogService}。
 */
@RequestMapping("/monitor")
/**
 * 日志控制器：操作日志与登录日志查询/清理。
 */
public class ErpLogController {

    private final ErpLogService logService;

    public ErpLogController(ErpLogService logService) {
        this.logService = logService;
    }

    // ===== 操作日志 =====

    @ApiName("操作日志列表")
    @ApiPermission("mcerp:monitor:operlog:list")
    @GetMapping("/operlog/list")
    public TableDataInfo operlogList(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                                     @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                     @RequestParam(name = "title", required = false) String title,
                                     @RequestParam(name = "operName", required = false) String operName) {
        return logService.operlogList(pageNum, pageSize, title, operName);
    }

    @ApiName("删除操作日志")
    @ApiPermission("mcerp:monitor:operlog:remove")
    @DeleteMapping("/operlog/{operIds}")
    public AjaxResult operlogRemove(@PathVariable(name = "operIds") String operIds) {
        return logService.operlogRemove(operIds);
    }

    @ApiName("清空操作日志")
    @ApiPermission("mcerp:monitor:operlog:clean")
    @DeleteMapping("/operlog/clean")
    public AjaxResult operlogClean() {
        return logService.operlogClean();
    }

    // ===== 登录日志 =====

    @ApiName("登录日志列表")
    @ApiPermission("mcerp:monitor:logininfor:list")
    @GetMapping("/logininfor/list")
    public TableDataInfo logininforList(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                                        @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                        @RequestParam(name = "userName", required = false) String userName,
                                        @RequestParam(name = "status", required = false) String status) {
        return logService.logininforList(pageNum, pageSize, userName, status);
    }

    @ApiName("删除登录日志")
    @ApiPermission("mcerp:monitor:logininfor:remove")
    @DeleteMapping("/logininfor/{infoIds}")
    public AjaxResult logininforRemove(@PathVariable(name = "infoIds") String infoIds) {
        return logService.logininforRemove(infoIds);
    }

    @ApiName("清空登录日志")
    @ApiPermission("mcerp:monitor:logininfor:clean")
    @DeleteMapping("/logininfor/clean")
    public AjaxResult logininforClean() {
        return logService.logininforClean();
    }

    @ApiName("解锁账号")
    @ApiPermission("mcerp:monitor:logininfor:unlock")
    @GetMapping("/logininfor/unlock/{userName}")
    public AjaxResult unlock(@PathVariable(name = "userName") String userName) {
        return logService.unlock(userName);
    }
}
