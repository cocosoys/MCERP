package com.github.cocosoys.mc.mcerp.entity;


import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * ERP 字典类型表。审计字段（createTime/updateTime 等）继承 {@link BaseEntity}。
 */
@TableName("erp_dict_type")
@Data
/**
 * 字典类型实体（erp_dict_type 表）。
 */
public class ErpDictType extends BaseEntity {

    /** 字典主键 ID */
    @TableId
    private String dictId;

    /** 字典名称 */
    private String dictName;

    /** 字典类型编码（唯一），如 system_normal_disable */
    private String dictType;

    /** 状态（0 正常 / 1 停用） */
    private String status;

    /** 备注 */
    private String remark;
}
