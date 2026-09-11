package com.github.cocosoys.mc.mcerp.entity;


import lombok.Data;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;

/**
 * 操作日志。
 */
@TableName("erp_oper_log")
@Data
public class SysOperLog {

    @TableId
    private String operId;

    private String title;

    /** 0其它 1新增 2修改 3删除 4授权 5导出 6导入 7强退 8生成代码 9清空数据 */
    private int businessType;

    private String method;

    private String requestMethod;

    private String operName;

    private String operUrl;

    private String operIp;

    private String operParam;

    private String jsonResult;

    /** 0成功 1失败 */
    private int status;

    private String errorMsg;

    private String operTime;
}
