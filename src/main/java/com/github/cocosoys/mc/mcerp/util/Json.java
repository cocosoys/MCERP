package com.github.cocosoys.mc.mcerp.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简 JSON 工具（无第三方依赖）：解析若依前端请求体（对象/数组）与序列化响应。
 * 覆盖：对象、数组、字符串、数字、布尔、null；不支持转义字符细节场景以外的高级特性（够用即可）。
 */
public final class Json {

    private Json() {
    }

    // ===== 解析 =====

    public static Object parse(String text) {
        if (text == null) {
            return null;
        }
        Parser p = new Parser(text.trim());
        Object v = p.parseValue();
        return v;
    }

    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        return v instanceof Map ? (Map<String, Object>) v : new LinkedHashMap<>();
    }

    public static List<Object> parseArray(String text) {
        Object v = parse(text);
        return v instanceof List ? (List<Object>) v : new ArrayList<>();
    }

    public static String getString(Map<String, Object> map, String key, String def) {
        Object v = map == null ? null : map.get(key);
        return v == null ? def : String.valueOf(v);
    }

    public static int getInt(Map<String, Object> map, String key, int def) {
        Object v = map == null ? null : map.get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public static long getLong(Map<String, Object> map, String key, long def) {
        Object v = map == null ? null : map.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v != null) {
            try {
                return Long.parseLong(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    // ===== 序列化 =====

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object o : (List<?>) v) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, o);
            }
            sb.append(']');
        } else if (v instanceof CharSequence) {
            writeString(sb, v.toString());
        } else if (v instanceof Boolean || v instanceof Number) {
            sb.append(v);
        } else {
            writeString(sb, String.valueOf(v));
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ===== 解析器 =====

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        Object parseValue() {
            skipWs();
            if (pos >= s.length()) {
                return null;
            }
            char c = s.charAt(pos);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                case 't':
                    expect("true");
                    return Boolean.TRUE;
                case 'f':
                    expect("false");
                    return Boolean.FALSE;
                case 'n':
                    expect("null");
                    return null;
                default:
                    return parseNumber();
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // {
            skipWs();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                if (peek() == ':') {
                    pos++;
                }
                Object v = parseValue();
                map.put(key, v);
                skipWs();
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == '}') {
                    pos++;
                    break;
                } else {
                    break;
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // [
            skipWs();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWs();
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == ']') {
                    pos++;
                    break;
                } else {
                    break;
                }
            }
            return list;
        }

        private String parseString() {
            if (peek() != '"') {
                return "";
            }
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '"') {
                    pos++;
                    break;
                }
                if (c == '\\' && pos + 1 < s.length()) {
                    char n = s.charAt(pos + 1);
                    switch (n) {
                        case '"':
                        case '\\':
                        case '/':
                            sb.append(n);
                            pos += 2;
                            continue;
                        case 'n':
                            sb.append('\n');
                            pos += 2;
                            continue;
                        case 'r':
                            sb.append('\r');
                            pos += 2;
                            continue;
                        case 't':
                            sb.append('\t');
                            pos += 2;
                            continue;
                        case 'u':
                            if (pos + 5 < s.length()) {
                                try {
                                    sb.append((char) Integer.parseInt(s.substring(pos + 2, pos + 6), 16));
                                    pos += 6;
                                    continue;
                                } catch (NumberFormatException ignored) {
                                }
                            }
                            break;
                        default:
                            break;
                    }
                    sb.append(n);
                    pos += 2;
                    continue;
                }
                sb.append(c);
                pos++;
            }
            return sb.toString();
        }

        private Object parseNumber() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            String num = s.substring(start, pos);
            try {
                if (num.indexOf('.') >= 0 || num.indexOf('e') >= 0 || num.indexOf('E') >= 0) {
                    return Double.parseDouble(num);
                }
                long l = Long.parseLong(num);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    return (int) l;
                }
                return l;
            } catch (NumberFormatException e) {
                return num;
            }
        }

        private void skipWs() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private char peek() {
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        private void expect(String word) {
            if (s.startsWith(word, pos)) {
                pos += word.length();
            }
        }
    }
}
