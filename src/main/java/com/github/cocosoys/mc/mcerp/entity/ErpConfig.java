package com.github.cocosoys.mc.mcerp.entity;


import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * ERP 参数配置表。审计字段（createTime/updateTime 等）继承 {@link BaseEntity}。
 */
@TableName("erp_config")
@Data
/**
 * 参数配置实体（erp_config 表）。
 */
public class ErpConfig extends BaseEntity {

    /** 参数主键 ID（主键） */
    @TableId
    private String configId;

    /** 参数名称 */
    private String configName;

    /** 参数键名，如 sys.account.captchaEnabled */
    private String configKey;

    /** 参数键值 */
    private String configValue;

    /** 系统内置（Y 系统内置 / N 非内置，前端据此隐藏删除按钮） */
    private String configType;

    /** 备注 */
    private String remark;
}
