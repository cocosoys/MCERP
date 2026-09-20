package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 树选择节点 VO（若依 treeselect 契约：{id, label, children} 实体化）。
 */
@Data
public class TreeselectVO {

    /** 节点 ID（对应菜单 menuId） */
    private String id;

    /** 节点标签（对应菜单名称） */
    private String label;

    /** 子节点（递归树选择结构） */
    private List<TreeselectVO> children = new ArrayList<>();

}
