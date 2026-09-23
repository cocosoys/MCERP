package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 分配权限请求体（savePerms）：permissions 为 SOYS 本地权限节点列表（menu perms / 数据权限 / 模块权限）。
 */
@Data
/**
 * 权限保存 VO：菜单 id + 权限标识列表。
 */
public class SavePermsVO {

    /** SOYS 本地权限节点列表（菜单 perms / 数据权限 / 模块权限，全量覆盖式保存） */
    private List<String> permissions;
}
