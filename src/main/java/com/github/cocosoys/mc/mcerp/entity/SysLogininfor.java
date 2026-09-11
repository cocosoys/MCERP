package com.github.cocosoys.mc.mcerp.entity;


import lombok.Data;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;

/**
 * 登录日志。
 */
@TableName("erp_logininfor")
@Data
public class SysLogininfor {

    @TableId
    private String infoId;

    private String userName;

    private String ipaddr;

    /** 0成功 1失败 */
    private String status;

    private String msg;

    private String loginTime;
}
