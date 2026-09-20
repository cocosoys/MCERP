package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.ErpConfig;
import com.github.cocosoys.mc.mcerp.service.ErpConfigService;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.DeleteMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 参数设置（若依契约）：路由 /api/plugins/MCERP/system/config/*。
 * CRUD 全部委托 {@link ErpConfigService}。
 */
@RequestMapping("/system/config")
public class ErpConfigController {

    private final ErpConfigService configService;

    public ErpConfigController(ErpConfigService configService) {
        this.configService = configService;
    }

    @ApiName("参数列表")
    @ApiPermission("system:config:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "configKey", required = false) String configKey,
                              @RequestParam(name = "configName", required = false) String configName) {
        return configService.list(pageNum, pageSize, configKey, configName);
    }

    @ApiName("参数详情")
    @ApiPermission("system:config:query")
    @GetMapping("/{configId}")
    public AjaxResult detail(@PathVariable(name = "configId") String configId) {
        return configService.detail(configId);
    }

    @ApiName("按键取参数")
    @ApiPermission("system:config:query")
    @GetMapping("/configKey/{configKey}")
    public AjaxResult byKey(@PathVariable(name = "configKey") String configKey) {
        return configService.byKey(configKey);
    }

    @ApiName("新增参数")
    @ApiPermission("system:config:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody ErpConfig config) {
        return configService.add(config);
    }

    @ApiName("编辑参数")
    @ApiPermission("system:config:edit")
    @PutMapping("/{configId}")
    public AjaxResult update(@PathVariable(name = "configId") String configId, @RequestBody ErpConfig config) {
        return configService.update(configId, config);
    }

    @ApiName("删除参数")
    @ApiPermission("system:config:remove")
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable(name = "configIds") String configIds) {
        return configService.remove(configIds);
    }

    @ApiName("刷新参数缓存")
    @ApiPermission("system:config:remove")
    @GetMapping("/refreshCache")
    public AjaxResult refreshCache() {
        return configService.refreshCache();
    }
}
