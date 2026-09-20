package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 分配角色请求体（authRoleSave）：userId + roleIds（SOYS 权限组 id 列表）。
 */
@Data
public class AuthRoleSaveVO {

    /** 用户 ID（erp_user.user_id） */
    private String userId;

    /** 分配的 SOYS 权限组 id 列表（全量覆盖式保存） */
    private List<String> roleIds;
}
