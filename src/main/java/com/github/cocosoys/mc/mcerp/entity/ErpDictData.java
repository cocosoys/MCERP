package com.github.cocosoys.mc.mcerp.entity;


import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * ERP 字典数据表。审计字段（createTime/updateTime 等）继承 {@link BaseEntity}。
 */
@TableName("erp_dict_data")
@Data
public class ErpDictData extends BaseEntity {

    /** 字典编码（主键） */
    @TableId
    private String dictCode;

    /** 字典排序（小在前） */
    private int dictSort;

    /** 字典标签（展示名） */
    private String dictLabel;

    /** 字典键值 */
    private String dictValue;

    /** 字典类型编码（关联 erp_dict_type.dict_type） */
    private String dictType;

    /** 状态（0 正常 / 1 停用） */
    private String status;

    /** 备注 */
    private String remark;
}
