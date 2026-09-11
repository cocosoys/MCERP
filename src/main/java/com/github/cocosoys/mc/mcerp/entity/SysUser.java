package com.github.cocosoys.mc.mcerp.entity;


import lombok.Data;
import com.dlz.db.annotation.TableField;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;

/**
 * ERP 后台用户（玩家）。密码不落库——登录校验完全交给 SOYS 登录桥（AuthMe）。
 */
@TableName("erp_user")
@Data
public class SysUser {

    @TableId
    private String userId;

    /** 玩家名（登录名） */
    private String userName;

    private String nickName;

    private String email;

    private String phonenumber;

    /** 性别 0男 1女 2未知 */
    private String sex;

    /** 状态 0正常 1停用 */
    private String status;

    private String remark;

    private String createTime;

    @TableField(exist = false)
    private transient boolean selected; // 前端角色分配用，不入库
}
