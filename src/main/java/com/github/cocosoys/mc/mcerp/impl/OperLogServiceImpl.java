package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.entity.ErpOperLog;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;

import java.util.UUID;

/**
 * 操作日志实现（从 ErpUserController.recordOper 剥离）：写入 erp_oper_log，
 * 失败静默吞异常——日志横切不阻断主业务。
 */
public class OperLogServiceImpl implements OperLogService {

    @Override
    public void record(String title, String business, String operName, String result) {
        try {
            ErpOperLog log = new ErpOperLog();
            log.setOperId(UUID.randomUUID().toString());
            log.setTitle(title);
            log.setBusinessType("新增".equals(business) ? 1 : "修改".equals(business) ? 2 : 3);
            log.setMethod(business);
            log.setRequestMethod("");
            log.setOperName(operName == null ? "" : operName);
            log.setOperUrl("");
            log.setOperIp("");
            log.setOperParam("");
            log.setJsonResult("");
            log.setStatus(0);
            log.setErrorMsg("");
            log.setOperTime(AuthService.now());
            DATA.insert(log);
        } catch (Exception ignored) {
        }
    }
}
