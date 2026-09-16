package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 树选择节点 VO（若依 treeselect 契约：{id, label, children} 实体化）。
 */
@Data
public class TreeselectVO {

    private String id;

    private String label;

    private List<TreeselectVO> children = new ArrayList<>();

}
