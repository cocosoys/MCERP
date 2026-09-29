package com.github.cocosoys.mc.mcerp.util;

/**
 * 主键解析工具：AUTO 自增数字主键的字符串路径参数 → {@link Long}。
 * <p>URL 路径参数（@PathVariable）为字符串形态，此处统一在 service/impl 层解析，
 * controller 保持零逻辑；null/空/非法输入返回 null，由上层按"记录不存在"处理。</p>
 */
public final class Ids {

    private Ids() {
    }

    /**
     * 解析自增主键：null / 空白 / 非数字 → null（上层按不存在处理）。
     *
     * @param s 路径参数中的主键字符串（如 "1"）
     * @return 解析后的主键，非法输入返回 null
     */
    public static Long parse(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
