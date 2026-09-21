package com.github.cocosoys.mc.mcerp.i18n;

import com.github.cocosoys.mc.soyshttpovermc.i18n.I18n;
import com.github.cocosoys.mc.soyshttpovermc.spi.Platforms;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * MCERP 国际化门面：桥接主插件 SOYSHTTPOverMC 的 {@link I18n} 机制。
 *
 * <p><b>注册</b>：{@link #register(JavaPlugin)} 在 {@code onEnable} 时调用——
 * 把 {@code resources/language/zh_cn.yml} 释放到 {@code plugins/MCERP/language/}，
 * 再经主插件 {@link I18n#registerLanguageSource} 注册为语言源（绑定 zh_cn，合并进
 * 默认作用域，注册后立即重载生效）。当前仅支持中文（zh_cn）。</p>
 *
 * <p><b>取值</b>：{@link #t(String, String, Object...)} 直接委托 {@link I18n#t}，
 * key 统一 {@code mcerp.} 前缀避免与主插件默认作用域键冲突；未命中 key 时回退
 * fallback（即内联中文原文），天然兜底。</p>
 */
public final class McerpI18n {

    /** 语言源名称（/soyshttp lang sources 展示用） */
    private static final String SOURCE_NAME = "mcerp";
    /** 语言源描述 */
    private static final String SOURCE_DESC = "MCERP 消息";
    /** 当前支持的语言（仅中文） */
    private static final String LANGUAGE = "zh_cn";

    private McerpI18n() {
    }

    /**
     * 注册 MCERP 语言包到主插件语言资源包（幂等思路：重复调用仅重复登记同名源，
     * 主插件按注册顺序覆盖同名键，最终文本一致）。
     *
     * @param plugin MCERP 主类（用于释放语言资源 + 定位语言文件夹）
     */
    public static void register(JavaPlugin plugin) {
        if (plugin == null) {
            return;
        }
        // 1. 释放 jar 内语言资源 → plugins/MCERP/language/zh_cn.yml（已存在则不覆盖，保留用户修改）
        try {
            plugin.saveResource("language/zh_cn.yml", false);
        } catch (IllegalArgumentException e) {
            // jar 内无该资源（异常情况）：跳过释放，后续注册仍可用已存在的文件
        }
        // 2. 注册语言源：language 文件夹按 <folder>/<语言>.yml 约定取 zh_cn.yml
        File folder = new File(plugin.getDataFolder(), "language");
        I18n.registerLanguageSource(
                Platforms.getOrNull(), SOURCE_NAME, SOURCE_DESC, LANGUAGE, folder.getAbsolutePath());
    }

    /**
     * 取翻译文本（默认作用域，key 须带 mcerp. 前缀）；未命中回退 fallback。
     *
     * @param key      i18n 键（如 {@code mcerp.user.not-found}）
     * @param fallback 回退文本（内联中文原文，未命中/语言包未加载时使用）
     * @param args     {@code {0} {1}...} 占位符参数
     */
    public static String t(String key, String fallback, Object... args) {
        return I18n.t(key, fallback, args);
    }
}
