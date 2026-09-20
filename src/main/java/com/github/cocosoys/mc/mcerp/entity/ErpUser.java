package com.github.cocosoys.mc.mcerp.entity;


import com.dlz.db.annotation.TableField;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * ERP 后台用户（玩家）。密码不落库——登录校验完全交给 SOYS 登录桥（AuthMe）。
 * 审计字段（createTime/updateTime 等）继承 {@link BaseEntity}。
 */
@TableName("erp_user")
@Data
public class ErpUser extends BaseEntity {

    /** 用户 ID（主键） */
    @TableId
    private String userId;

    /** 玩家名（登录名） */
    private String userName;

    /** 昵称 */
    private String nickName;

    /** 邮箱 */
    private String email;

    /** 手机号码 */
    private String phonenumber;

    /** 性别（0 男 / 1 女 / 2 未知） */
    private String sex;

    /** 账号状态（0 正常 / 1 停用） */
    private String status;

    /** 备注 */
    private String remark;

    /** 前端角色分配勾选用，不入库 */
    @TableField(exist = false)
    private transient boolean selected;
}
