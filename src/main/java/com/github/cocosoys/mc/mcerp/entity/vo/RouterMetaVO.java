package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 路由元信息 VO（对应前端 vue-router meta 字段）。
 */
@Data
public class RouterMetaVO {

    /** 菜单标题 */
    private String title;

    /** 菜单图标 */
    private String icon;

    /** 是否缓存（0缓存/1不缓存） */
    private Boolean noCache;

    /** 外链地址（内部菜单为 null） */
    private String link;
}
