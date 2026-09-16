package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户权限列表 VO（若依 /permissions/{userName} 契约：all/direct/owned 实体化）。
 */
@Data
public class UserPermsVO {

    /** 可分配权限节点全集 */
    private List<String> all;

    /** 用户直接权限节点（仅 USER 类型，含否定前缀还原） */
    private List<String> direct;

    /** 用户有效权限节点（组继承 + 直接） */
    private List<String> owned;

}
