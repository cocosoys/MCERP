package com.github.cocosoys.mc.mcerp.entity;


import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.cocosoys.mc.soyshttpovermc.orm.convertor.BeanCodec;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * ERP 操作日志表。审计字段（createTime/updateTime 等）继承 {@link BaseEntity}。
 */
@TableName("erp_oper_log")
@Data
public class ErpOperLog extends BaseEntity {

    /** 日志主键 ID */
    @TableId
    private String operId;

    /** 模块标题（如 用户管理） */
    private String title;

    /** 业务类型（0其它 1新增 2修改 3删除 4授权 5导出 6导入 7强退 8生成代码 9清空数据） */
    private int businessType;

    /** 方法名称（类.方法） */
    private String method;

    /** 请求方式（GET/POST/...） */
    private String requestMethod;

    /** 操作人员（玩家名） */
    private String operName;

    /** 请求 URL */
    private String operUrl;

    /** 主机地址（IP） */
    private String operIp;

    /** 请求参数 */
    private String operParam;

    /** 返回参数 */
    private String jsonResult;

    /** 操作状态（0 成功 / 1 失败） */
    private int status;

    /** 错误消息 */
    private String errorMsg;

    /** 操作时间（yyyy-MM-dd HH:mm:ss）。 */
    @JsonFormat(pattern = BeanCodec.DATE_TIME_PATTERN)
    private Date operTime;
}
