package com.github.cocosoys.mc.mcerp.entity;


import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * ERP 通知公告表。审计字段（createTime/updateTime 等）继承 {@link BaseEntity}。
 */
@TableName("erp_notice")
@Data
/**
 * 通知公告实体（erp_notice 表）。
 */
public class ErpNotice extends BaseEntity {

    /** 公告 ID（主键） */
    @TableId
    private String noticeId;

    /** 公告标题 */
    private String noticeTitle;

    /** 公告类型（1 通知 / 2 公告） */
    private String noticeType;

    /** 公告内容 */
    private String noticeContent;

    /** 公告状态（0 正常 / 1 关闭） */
    private String status;

    /** 创建者（子类遮蔽基类 exist=false 版本，落库） */
    private String createBy;
}
