package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.entity.SysConfig;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 参数设置服务（抽象契约）：参数 CRUD。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.SysConfigServiceImpl}；
 * 内置参数（configType=Y）不可改删由 impl 负责。
 */
public interface SysConfigService {

    TableDataInfo list(Integer pageNum, Integer pageSize, String configKey, String configName);

    AjaxResult detail(String configId);

    AjaxResult byKey(String configKey);

    AjaxResult add(SysConfig config);

    AjaxResult update(String configId, SysConfig config);

    AjaxResult remove(String configIds);

    AjaxResult refreshCache();
}
