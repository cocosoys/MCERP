package com.github.cocosoys.mc.mcerp.entity;


import lombok.Data;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;

/**
 * 参数设置。
 */
@TableName("erp_config")
@Data
public class SysConfig {

    @TableId
    private String configId;

    private String configName;

    /** 参数键名，如 sys.account.captchaEnabled */
    private String configKey;

    private String configValue;

    /** Y 系统内置 / N 非内置 */
    private String configType;

    private String remark;

    private String createTime;
}
