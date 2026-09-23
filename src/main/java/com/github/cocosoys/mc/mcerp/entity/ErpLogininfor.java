package com.github.cocosoys.mc.mcerp.entity;


import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.cocosoys.mc.soyshttpovermc.orm.convertor.BeanCodec;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * ERP 登录日志表。审计字段（createTime/updateTime 等）继承 {@link BaseEntity}。
 */
@TableName("erp_logininfor")
@Data
/**
 * 登录日志实体（erp_logininfor 表）。
 */
public class ErpLogininfor extends BaseEntity {

    /** 访问 ID（主键） */
    @TableId
    private String infoId;

    /** 用户账号 */
    private String userName;

    /** 登录 IP 地址 */
    private String ipaddr;

    /** 登录状态（0 成功 / 1 失败） */
    private String status;

    /** 提示消息 */
    private String msg;

    /** 登录时间（yyyy-MM-dd HH:mm:ss）。 */
    @JsonFormat(pattern = BeanCodec.DATE_TIME_PATTERN)
    private Date loginTime;
}
