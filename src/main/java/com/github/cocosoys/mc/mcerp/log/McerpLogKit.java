package com.github.cocosoys.mc.mcerp.log;

import com.github.cocosoys.mc.soyshttpovermc.log.LogKit;

/**
 * MCERP 日志门面：继承 SOYS LogKit（统一经其全局级别过滤与热重载打印），
 * 定制本插件前缀 [MCERP]。作为 Lombok 自定义日志的工厂（lombok.config:
 * {@code lombok.log.custom.declaration = ...McerpLogKit McerpLogKit.getLogger(TYPE)}），
 * 使用类加 {@code @CustomLog} 即可生成 {@code static final McerpLogKit log}。
 */
public class McerpLogKit extends LogKit {

    public McerpLogKit(String prefix, Class<?> sourceClass) {
        super(prefix, sourceClass);
    }

    /**
     * Lombok 自定义日志工厂（无 topic 重载）：前缀 [MCERP]。
     */
    public static McerpLogKit getLogger(Class<?> clazz) {
        return new McerpLogKit("[MCERP]", clazz);
    }

    /**
     * Lombok 自定义日志工厂（带 topic 重载）：前缀 [topic]，用于多主题细分。
     */
    public static McerpLogKit getLogger(Class<?> clazz, String topic) {
        return new McerpLogKit("[" + topic + "]", clazz);
    }
}
