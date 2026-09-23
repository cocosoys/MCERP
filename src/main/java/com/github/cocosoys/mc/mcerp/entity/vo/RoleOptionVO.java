package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 角色下拉选项 VO（若依 authRole/detail 契约的 {roleId, roleName, status} 实体化）。
 */
@Data
/**
 * 角色选项 VO：角色 id + 名称。
 */
public class RoleOptionVO {

    /** 角色（SOYS 权限组）id */
    private String roleId;

    /** 角色展示名 */
    private String roleName;

    /** 状态（若依契约固定 '0' 正常） */
    private String status;
}
