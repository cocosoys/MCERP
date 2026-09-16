package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户详情 VO（若依 user detail 契约：平铺 user 字段 + roles/roleIds/postIds 实体化）。
 */
@Data
public class UserDetailVO {

    private String userId;
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;
    private String sex;
    private String status;
    private String remark;
    private String createTime;

    /** 角色下拉选项 */
    private List<RoleOptionVO> roles;

    /** 已分配角色（SOYS 权限组）id 列表 */
    private List<String> roleIds;

    /** 岗位 id（模块未实现，空列表） */
    private List<String> postIds;
}
