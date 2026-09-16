package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 分配角色请求体（authRoleSave）：userId + roleIds（SOYS 权限组 id 列表）。
 */
@Data
public class AuthRoleSaveVO {

    private String userId;
    private List<String> roleIds;
}
