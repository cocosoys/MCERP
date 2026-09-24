package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由节点 VO（对应前端 vue-router 路由记录）。
 */
@Data
public class RouterVO {

    /** 路由名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 是否隐藏 */
    private Boolean hidden;

    /** 重定向 */
    private String redirect;

    /** 是否总是显示（目录展开） */
    private Boolean alwaysShow;

    /** 组件路径（Layout / ParentView / iframe:xxx / wujie:xxx） */
    private String component;

    /** 路由元信息 */
    private RouterMetaVO meta;

    /** 子路由 */
    private List<RouterVO> children = new ArrayList<>();
}
