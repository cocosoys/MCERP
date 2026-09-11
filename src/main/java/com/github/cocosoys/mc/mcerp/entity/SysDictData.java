package com.github.cocosoys.mc.mcerp.entity;


import lombok.Data;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;

/**
 * 字典数据。
 */
@TableName("erp_dict_data")
@Data
public class SysDictData {

    @TableId
    private String dictCode;

    private int dictSort;

    private String dictLabel;

    private String dictValue;

    private String dictType;

    private String status;

    private String remark;
}
