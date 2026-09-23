package com.github.cocosoys.mc.mcerp.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.cocosoys.mc.soyshttpovermc.orm.convertor.BeanCodec;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 用户详情 VO（若依 user detail 契约：平铺 user 字段 + roles/roleIds/postIds 实体化）。
 */
@Data
/**
 * 用户详情 VO：用户完整信息。
 */
public class UserDetailVO {

    /** 用户 ID（erp_user.user_id，SOYS 玩家 UUID） */
    private String userId;

    /** 登录名（SOYS 玩家名） */
    private String userName;

    /** 昵称（展示名） */
    private String nickName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phonenumber;

    /** 性别（字典 sys_user_sex：0 男 / 1 女 / 2 未知） */
    private String sex;

    /** 账号状态：'0' 正常 / '1' 停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间（yyyy-MM-dd HH:mm:ss）。 */
    @JsonFormat(pattern = BeanCodec.DATE_TIME_PATTERN)
    private Date createTime;

    /** 角色下拉选项 */
    private List<RoleOptionVO> roles;

    /** 已分配角色（SOYS 权限组）id 列表 */
    private List<String> roleIds;

    /** 岗位 id（模块未实现，空列表） */
    private List<String> postIds;
}
