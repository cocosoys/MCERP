package com.github.cocosoys.mc.mcerp.entity.vo;

import com.github.cocosoys.mc.mcerp.entity.ErpUser;
import lombok.Data;

import java.util.List;

/**
 * 用户角色/权限编辑页 VO（若依 authRole 契约：roles/roleIds/allPerms/perms/user 实体化）。
 */
@Data
public class UserAuthRoleVO {

    /** 角色下拉选项（SOYS 权限组） */
    private List<RoleOptionVO> roles;

    /** 已分配角色 id 列表 */
    private List<String> roleIds;

    /** 可分配权限节点全集（菜单 perms + 数据权限 + 模块权限） */
    private List<String> allPerms;

    /** 菜单权限树（权限分配弹窗 el-tree 数据，含 perms/menuType） */
    private List<MenuTreeVO> menuTree;

    /** 用户直接权限节点（仅 USER 类型，不含组继承） */
    private List<String> perms;

    /** 用户实体 */
    private ErpUser user;

}
