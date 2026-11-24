package com.github.cocosoys.mc.mcerp.enums;

/**
 * 操作日志业务类型枚举（与前端字典 {@code sys_oper_type} 口径一致）。
 *
 * <p>原实现从中文展示文案（新增/修改/删除）推断类型码，英文或非中文环境恒为 0；
 * 改为调用方显式传入本枚举，{@link #getCode()} 即写入 {@code erp_oper_log.business_type} 的类型码，
 * 不再依赖任何语言环境。</p>
 *
 * <p>类型码口径：0 其它 / 1 新增 / 2 修改 / 3 删除 / 4 授权 / 5 导出 / 6 导入 /
 * 7 强退 / 8 生成代码 / 9 清空数据。</p>
 */
public enum BusinessType {

    /** 其它（0） */
    OTHER(0, "其它"),
    /** 新增（1） */
    INSERT(1, "新增"),
    /** 修改（2） */
    UPDATE(2, "修改"),
    /** 删除（3） */
    DELETE(3, "删除"),
    /** 授权（4） */
    GRANT(4, "授权"),
    /** 导出（5） */
    EXPORT(5, "导出"),
    /** 导入（6） */
    IMPORT(6, "导入"),
    /** 强退（7） */
    FORCE(7, "强退"),
    /** 生成代码（8） */
    GEN_CODE(8, "生成代码"),
    /** 清空数据（9） */
    CLEAN(9, "清空数据");

    /** 写入数据库的类型码（与 sys_oper_type 字典 value 一致） */
    private final int code;

    /** 中文展示标签（仅用于日志/文档，不参与类型判定） */
    private final String label;

    BusinessType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 获取类型码。
     *
     * @return 0..9 类型码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取中文展示标签。
     *
     * @return 标签文案
     */
    public String getLabel() {
        return label;
    }

    /**
     * 按类型码解析枚举；未命中（含未知码）回退 {@link #OTHER}，保证不返回 null。
     *
     * @param code 类型码
     * @return 对应枚举；无匹配时为 OTHER
     */
    public static BusinessType fromCode(int code) {
        for (BusinessType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return OTHER;
    }
}
