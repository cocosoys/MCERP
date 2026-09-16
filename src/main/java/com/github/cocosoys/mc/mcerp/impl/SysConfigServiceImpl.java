package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.entity.SysConfig;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.SysConfigService;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 参数设置实现（从 SysConfigController 迁入）：CRUD + 按 key 查询；
 * 内置参数（configType=Y）不可修改/删除。
 */
public class SysConfigServiceImpl implements SysConfigService {

    private final OperLogService operLog;

    public SysConfigServiceImpl(OperLogService operLog) {
        this.operLog = operLog;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String configKey, String configName) {
        List<SysConfig> all = DATA.select(SysConfig.class);
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
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(String configId) {
        SysConfig c = DATA.get(SysConfig.class, configId);
        if (c == null) {
            return AjaxResult.error("参数不存在");
        }
        return AjaxResult.success(c);
    }

    @Override
    public AjaxResult byKey(String configKey) {
        if (configKey == null) {
            return AjaxResult.error("缺少 configKey");
        }
        // 若依契约：getConfigKey 的响应 msg 携带配置值（前端 user/index.vue 读 response.msg 作为初始密码）
        for (SysConfig c : DATA.select(SysConfig.class)) {
            if (configKey.equals(c.getConfigKey())) {
                AjaxResult ok = AjaxResult.success();
                ok.put("msg", c.getConfigValue() == null ? "" : c.getConfigValue());
                return ok;
            }
        }
        // 未配置：返回空值而非 error，避免前端弹出"参数不存在"
        AjaxResult ok = AjaxResult.success();
        ok.put("msg", "");
        return ok;
    }

    @Override
    public AjaxResult add(SysConfig config) {
        String key = config.getConfigKey() == null ? "" : config.getConfigKey().trim();
        if (key.isEmpty()) {
            return AjaxResult.error("参数键名不能为空");
        }
        for (SysConfig c : DATA.select(SysConfig.class)) {
            if (key.equals(c.getConfigKey())) {
                return AjaxResult.error("参数键名已存在");
            }
        }
        SysConfig c = new SysConfig();
        c.setConfigId(UUID.randomUUID().toString());
        c.setConfigName(config.getConfigName() == null || config.getConfigName().isEmpty() ? key : config.getConfigName());
        c.setConfigKey(key);
        c.setConfigValue(config.getConfigValue() == null ? "" : config.getConfigValue());
        c.setConfigType(config.getConfigType() == null || config.getConfigType().isEmpty() ? "N" : config.getConfigType());
        c.setRemark(config.getRemark() == null ? "" : config.getRemark());
        c.setCreateTime(AuthService.now());
        DATA.insert(c);
        operLog.record("参数设置", "新增参数", key, "新增成功");
        return AjaxResult.success("新增成功");
    }

    @Override
    public AjaxResult update(String configId, SysConfig config) {
        SysConfig c = configId == null || configId.isEmpty() ? null : DATA.get(SysConfig.class, configId);
        if (c == null) {
            return AjaxResult.error("参数不存在");
        }
        if ("Y".equals(c.getConfigType())) {
            return AjaxResult.error("系统内置参数不可修改");
        }
        if (config.getConfigName() != null) {
            c.setConfigName(config.getConfigName());
        }
        if (config.getConfigValue() != null) {
            c.setConfigValue(config.getConfigValue());
        }
        if (config.getRemark() != null) {
            c.setRemark(config.getRemark());
        }
        DATA.updateById(c);
        operLog.record("参数设置", "修改参数", c.getConfigKey(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @Override
    public AjaxResult remove(String configIds) {
        if (configIds == null || configIds.isEmpty()) {
            return AjaxResult.error("缺少 configId");
        }
        for (String id : configIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysConfig c = DATA.get(SysConfig.class, id.trim());
            if (c != null && "Y".equals(c.getConfigType())) {
                return AjaxResult.error("系统内置参数不可删除");
            }
            DATA.deleteById(SysConfig.class, id.trim());
            if (c != null) {
                operLog.record("参数设置", "删除参数", c.getConfigKey(), "删除成功");
            }
        }
        return AjaxResult.success("删除成功");
    }

    @Override
    public AjaxResult refreshCache() {
        return AjaxResult.success("刷新成功");
    }

    // ===== 内部 =====

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
