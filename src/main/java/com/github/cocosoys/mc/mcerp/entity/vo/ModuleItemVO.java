package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 模块列表项 VO。
 */
@Data
public class ModuleItemVO {

    /** 模块标识 */
    private String id;

    /** 模块名称 */
    private String name;

    /** 类型：main / addon */
    private String type;

    /** 菜单数 */
    private int menus;
}
