package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.SysConfig;
import com.github.cocosoys.mc.mcerp.service.Store;
import com.github.cocosoys.mc.mcerp.util.Json;
import com.github.cocosoys.mc.mcerp.util.TableDataInfo;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 参数设置（若依契约：经 registerProxyController 代理注册，SOYS 自动补 /api 全局前缀
 * → 实际路由 /api/prod-api/system/config/*）。
 */
@RequestMapping("/prod-api/system/config")
public class SysConfigController {

    public SysConfigController() {
    }

    /** 内置参数（首次启动写入） */
    public void ensureBuiltinConfigs() {
        ensure("sys.account.captchaEnabled", "验证码开关", "false");
        ensure("sys.account.registerUser", "注册用户开关", "false");
    }

    private void ensure(String key, String name, String value) {
        for (SysConfig c : Store.select(SysConfig.class)) {
            if (key.equals(c.getConfigKey())) {
                return;
            }
        }
        SysConfig c = new SysConfig();
        c.setConfigId(UUID.randomUUID().toString());
        c.setConfigName(name);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setConfigType("Y");
        c.setRemark("系统内置参数");
        c.setCreateTime(com.github.cocosoys.mc.mcerp.service.AuthService.now());
        Store.insert(c);
    }

    @ApiName("参数列表")
    @ApiPermission("system:config:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "configKey", required = false) String configKey,
                              @RequestParam(name = "configName", required = false) String configName) {
        List<SysConfig> all = Store.select(SysConfig.class);
        List<SysConfig> filtered = new ArrayList<>();
        for (SysConfig c : all) {
            if (configKey != null && !configKey.isEmpty() && !contains(c.getConfigKey(), configKey)) {
                continue;
            }
            if (configName != null && !configName.isEmpty() && !contains(c.getConfigName(), configName)) {
                continue;
            }
            filtered.add(c);
        }
        filtered.sort(Comparator.comparing(SysConfig::getCreateTime, Comparator.nullsLast(String::compareTo)).reversed());
        return SysUserController.page(filtered, pageNum, pageSize);
    }

    @ApiName("参数详情")
    @ApiPermission("system:config:query")
    @GetMapping("/{configId}")
    public AjaxResult detail(@PathVariable(name = "configId") String configId) {
        SysConfig c = Store.get(SysConfig.class, configId);
        if (c == null) {
            return AjaxResult.error("参数不存在");
        }
        return AjaxResult.success(c);
    }

    @ApiName("按键取参数")
    @ApiPermission("system:config:query")
    @GetMapping("/configKey/{configKey}")
    public AjaxResult byKey(@PathVariable(name = "configKey") String configKey) {
        if (configKey == null) {
            return AjaxResult.error("缺少 configKey");
        }
        for (SysConfig c : Store.select(SysConfig.class)) {
            if (configKey.equals(c.getConfigKey())) {
                return AjaxResult.success(c);
            }
        }
        return AjaxResult.error("参数不存在");
    }

    @ApiName("新增参数")
    @ApiPermission("system:config:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        String key = Json.getString(map, "configKey", "");
        if (key.isEmpty()) {
            return AjaxResult.error("参数键名不能为空");
        }
        for (SysConfig c : Store.select(SysConfig.class)) {
            if (key.equals(c.getConfigKey())) {
                return AjaxResult.error("参数键名已存在");
            }
        }
        SysConfig c = new SysConfig();
        c.setConfigId(UUID.randomUUID().toString());
        c.setConfigName(Json.getString(map, "configName", key));
        c.setConfigKey(key);
        c.setConfigValue(Json.getString(map, "configValue", ""));
        c.setConfigType(Json.getString(map, "configType", "N"));
        c.setRemark(Json.getString(map, "remark", ""));
        c.setCreateTime(com.github.cocosoys.mc.mcerp.service.AuthService.now());
        Store.insert(c);
        SysUserController.recordOper("参数设置", "新增参数", key, "新增成功");
        return AjaxResult.success("新增成功");
    }

    @ApiName("编辑参数")
    @ApiPermission("system:config:edit")
    @PutMapping("/{configId}")
    public AjaxResult update(@PathVariable(name = "configId") String configId, @RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        SysConfig c = configId == null || configId.isEmpty() ? null : Store.get(SysConfig.class, configId);
        if (c == null) {
            return AjaxResult.error("参数不存在");
        }
        if ("Y".equals(c.getConfigType())) {
            return AjaxResult.error("系统内置参数不可修改");
        }
        c.setConfigName(Json.getString(map, "configName", c.getConfigName()));
        c.setConfigValue(Json.getString(map, "configValue", c.getConfigValue()));
        c.setRemark(Json.getString(map, "remark", c.getRemark()));
        Store.updateById(c);
        SysUserController.recordOper("参数设置", "修改参数", c.getConfigKey(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @ApiName("删除参数")
    @ApiPermission("system:config:remove")
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable(name = "configIds") String configIds) {
        if (configIds == null || configIds.isEmpty()) {
            return AjaxResult.error("缺少 configId");
        }
        for (String id : configIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysConfig c = Store.get(SysConfig.class, id.trim());
            if (c != null && "Y".equals(c.getConfigType())) {
                return AjaxResult.error("系统内置参数不可删除");
            }
            Store.deleteById(SysConfig.class, id.trim());
            if (c != null) {
                SysUserController.recordOper("参数设置", "删除参数", c.getConfigKey(), "删除成功");
            }
        }
        return AjaxResult.success("删除成功");
    }

    @ApiName("刷新参数缓存")
    @ApiPermission("system:config:remove")
    @GetMapping("/refreshCache")
    public AjaxResult refreshCache() {
        return AjaxResult.success("刷新成功");
    }

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
