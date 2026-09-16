package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 字典选项（dataByType 响应项）：若依前端 dict 下拉所需的三字段。
 */
@Data
public class DictOptionVO {

    private String dictLabel;
    private String dictValue;
    private String dictType;
}
