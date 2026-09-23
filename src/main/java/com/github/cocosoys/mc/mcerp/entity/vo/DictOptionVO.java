package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 字典选项（dataByType 响应项）：若依前端 dict 下拉所需的三字段。
 */
@Data
/**
 * 字典选项 VO：标签 + 值。
 */
public class DictOptionVO {

    /** 字典标签（下拉展示文案） */
    private String dictLabel;

    /** 字典值（提交到后端的数据值） */
    private String dictValue;

    /** 字典类型（所属字典组，如 sys_user_sex） */
    private String dictType;
}
