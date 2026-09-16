package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单权限树节点 VO（权限分配弹窗的 el-tree 数据：menuId/parentId/menuName/perms/menuType/children）。
 */
@Data
public class MenuTreeVO {

    private String menuId;
    private String parentId;
    private String menuName;
    private String perms;
    private String menuType;
    private List<MenuTreeVO> children = new ArrayList<>();

}
