package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 个人中心 VO（若依 /profile 契约：user/roleGroup/postGroup 实体化）。
 */
@Data
public class ProfileVO {

    /** 用户信息（复用 getInfo 的 user 结构） */
    private Object user;

    /** 角色组展示（OP=超级管理员，其余=普通玩家） */
    private String roleGroup;

    /** 岗位组展示 */
    private String postGroup;

}
