package com.github.cocosoys.mc.mcerp.entity;


import lombok.Data;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;

/**
 * 通知公告。
 */
@TableName("erp_notice")
@Data
public class SysNotice {

    @TableId
    private String noticeId;

    private String noticeTitle;

    /** 1 通知 / 2 公告 */
    private String noticeType;

    private String noticeContent;

    /** 0 正常 / 1 关闭 */
    private String status;

    private String createBy;

    private String createTime;
}
