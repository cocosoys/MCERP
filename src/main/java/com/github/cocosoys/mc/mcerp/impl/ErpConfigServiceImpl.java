package com.github.cocosoys.mc.mcerp.impl;

import static com.github.cocosoys.mc.mcerp.i18n.McerpI18n.t;

import com.github.cocosoys.mc.mcerp.entity.ErpConfig;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.ErpConfigService;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 参数设置实现（从 ErpConfigController 迁入）：CRUD + 按 key 查询；
 * 内置参数（configType=Y）不可修改/删除。
 */
public class ErpConfigServiceImpl implements ErpConfigService {

    private final OperLogService operLog;

    public ErpConfigServiceImpl(OperLogService operLog) {
        this.operLog = operLog;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String configKey, String configName) {
        List<ErpConfig> all = DATA.select(ErpConfig.class);
        List<ErpConfig> filtered = new ArrayList<>();
        for (ErpConfig c : all) {
            if (configKey != null && !configKey.isEmpty() && !contains(c.getConfigKey(), configKey)) {
                continue;
            }
            if (configName != null && !configName.isEmpty() && !contains(c.getConfigName(), configName)) {
                continue;
            }
            filtered.add(c);
        }
        filtered.sort(Comparator.comparing(ErpConfig::getCreateTime, Comparator.nullsLast(Date::compareTo)).reversed());
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(String configId) {
        ErpConfig c = DATA.get(ErpConfig.class, configId);
        if (c == null) {
            return AjaxResult.error(t("mcerp.config.not-found", "参数不存在"));
        }
        return AjaxResult.success(c);
    }

    @Override
    public AjaxResult byKey(String configKey) {
        if (configKey == null) {
            return AjaxResult.error(t("mcerp.config.missing-key", "缺少 configKey"));
        }
        // 若依契约：getConfigKey 的响应 msg 携带配置值（前端 user/index.vue 读 response.msg 作为初始密码）
        for (ErpConfig c : DATA.select(ErpConfig.class)) {
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
    public AjaxResult add(ErpConfig config) {
        String key = config.getConfigKey() == null ? "" : config.getConfigKey().trim();
        if (key.isEmpty()) {
            return AjaxResult.error(t("mcerp.config.key-empty", "参数键名不能为空"));
        }
        for (ErpConfig c : DATA.select(ErpConfig.class)) {
            if (key.equals(c.getConfigKey())) {
                return AjaxResult.error(t("mcerp.config.key-exists", "参数键名已存在"));
            }
        }
        ErpConfig c = new ErpConfig();
        c.setConfigId(UUID.randomUUID().toString());
        c.setConfigName(config.getConfigName() == null || config.getConfigName().isEmpty() ? key : config.getConfigName());
        c.setConfigKey(key);
        c.setConfigValue(config.getConfigValue() == null ? "" : config.getConfigValue());
        c.setConfigType(config.getConfigType() == null || config.getConfigType().isEmpty() ? "N" : config.getConfigType());
        c.setRemark(config.getRemark() == null ? "" : config.getRemark());
        c.setCreateTime(AuthService.now());
        DATA.insert(c);
        operLog.record(t("mcerp.operlog.module.config", "参数设置"), t("mcerp.operlog.action.add-config", "新增参数"), key, t("mcerp.common.add-success", "新增成功"));
        return AjaxResult.success(t("mcerp.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(String configId, ErpConfig config) {
        ErpConfig c = configId == null || configId.isEmpty() ? null : DATA.get(ErpConfig.class, configId);
        if (c == null) {
            return AjaxResult.error(t("mcerp.config.not-found", "参数不存在"));
        }
        if ("Y".equals(c.getConfigType())) {
            return AjaxResult.error(t("mcerp.config.builtin-readonly", "系统内置参数不可修改"));
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
        operLog.record(t("mcerp.operlog.module.config", "参数设置"), t("mcerp.operlog.action.edit-config", "修改参数"), c.getConfigKey(), t("mcerp.common.edit-success", "修改成功"));
        return AjaxResult.success(t("mcerp.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String configIds) {
        if (configIds == null || configIds.isEmpty()) {
            return AjaxResult.error(t("mcerp.config.missing-id", "缺少 configId"));
        }
        for (String id : configIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            ErpConfig c = DATA.get(ErpConfig.class, id.trim());
            if (c != null && "Y".equals(c.getConfigType())) {
                return AjaxResult.error(t("mcerp.config.builtin-nodelete", "系统内置参数不可删除"));
            }
            DATA.deleteById(ErpConfig.class, id.trim());
            if (c != null) {
                operLog.record(t("mcerp.operlog.module.config", "参数设置"), t("mcerp.operlog.action.delete-config", "删除参数"), c.getConfigKey(), t("mcerp.common.delete-success", "删除成功"));
            }
        }
        return AjaxResult.success(t("mcerp.common.delete-success", "删除成功"));
    }

    @Override
    public AjaxResult refreshCache() {
        return AjaxResult.success(t("mcerp.common.refresh-success", "刷新成功"));
    }

    // ===== 内部 =====

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
