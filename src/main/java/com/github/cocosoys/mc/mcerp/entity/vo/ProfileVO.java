package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 个人中心 VO（若依 /profile 契约：user/roleGroup 实体化）。
 */
@Data
/**
 * 个人信息 VO：当前登录者资料。
 */
public class ProfileVO {

    /** 用户信息（复用 getInfo 的 user 结构） */
    private Object user;

    /** 角色组展示（OP=超级管理员，其余=普通玩家） */
    private String roleGroup;


}
