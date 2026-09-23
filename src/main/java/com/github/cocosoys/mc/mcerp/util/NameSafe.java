package com.github.cocosoys.mc.mcerp.util;

/**
 * 名称安全化工具：把任意字符串转换为合法的 JS 标识符/路由 name（仅字母数字下划线）。
 *
 * <p>用于插件 identifier → 路由 path/组件名的转换，例如：</p>
 * <ul>
 *   <li>{@code SOYSHTTPOverMC-ERP} → {@code SOYSHTTPOverMCERP}（去掉横线）</li>
 *   <li>{@code SOYSHTTPOverMCERP} → {@code soyshttpovermcerp}（再 toLowerCase 做 path）</li>
 * </ul>
 *
 * <p>规则：只保留字母、数字、下划线；横线/空格/点等特殊字符被丢弃。
 * 结果为空时返回 {@code "item"} 兜底。</p>
 */
public final class NameSafe {

    private NameSafe() {
    }

    /**
     * 过滤非法字符：只保留字母、数字、下划线。
     *
     * @param s 原始字符串（如插件 identifier）
     * @return 安全化后的字符串；输入为 null/空或过滤后为空时返回 "item"
     */
    public static String safe(String s) {
        if (s == null || s.isEmpty()) {
            return "item";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "item" : sb.toString();
    }

    /**
     * 安全化 + 转小写（用于路由 path）。
     *
     * @param s 原始字符串
     * @return 小写安全化字符串
     */
    public static String path(String s) {
        return safe(s).toLowerCase();
    }
}
