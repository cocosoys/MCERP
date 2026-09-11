package com.github.cocosoys.mc.mcerp.entity;


import lombok.Data;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;

/**
 * 字典类型。
 */
@TableName("erp_dict_type")
@Data
public class SysDictType {

    @TableId
    private String dictId;

    private String dictName;

    /** 字典类型编码（唯一），如 system_normal_disable */
    private String dictType;

    private String status;

    private String remark;

    private String createTime;
}
