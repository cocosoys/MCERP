package com.github.cocosoys.mc.mcerp.web;

import com.github.cocosoys.mc.soyshttpovermc.api.SoysHttpOverMcApi;
import com.github.cocosoys.mc.soyshttpovermc.api.WebPageApi;
import lombok.CustomLog;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * RuoYi-Vue 前端（dist）托管：
 * <ol>
 *   <li>整目录经正常登记（非 proxy）挂到 SOYS 命名空间：registerResourceDirectory 自动补
 *       {@code /plugins/<插件名>} 前缀（插件名 = MCERP）→ 实际地址 /plugins/MCERP/**（jar 内 /dist/**）</li>
 *   <li>改写 index.html 资源引用（/static/→{@link #PREFIX}/static/、/favicon.ico→{@link #PREFIX}/favicon.ico），
 *       与 SOYS 自动补全的页面前缀保持一致</li>
 *   <li>改写所有 JS 中 axios baseURL「/prod-api」→「{SOYS api-prefix}/prod-api」：
 *       SOYS ApiRegistry 注册时对每个端点自动 applyPrefix 全局前缀（默认 /api），
 *       故注册表路由为 /api/prod-api/*；前端 baseURL 需对齐为 /api/prod-api 才能命中
 *       （upload 组件 baseUrl 同被替换）</li>
 * </ol>
 * 入口：http://&lt;host&gt;/plugins/MCERP/index.html（登录后为 SPA 内部导航，不触发服务端请求）。
 */
@CustomLog
public class DistHost {

    /** SOYS 正常登记自动补全的页面命名空间前缀：/plugins/&lt;插件名&gt;（插件名 MCERP） */
    public static final String PREFIX = "/plugins/MCERP";

    private DistHost() {
    }

    public static void host(JavaPlugin plugin, SoysHttpOverMcApi soysApi) {
        WebPageApi webPage = soysApi.getWebPage();
        String apiPrefix = soysApi.getApiRegistration().getApiPrefix();
        if (apiPrefix == null || apiPrefix.trim().isEmpty() || "/".equals(apiPrefix.trim())) {
            apiPrefix = "";
        } else {
            apiPrefix = apiPrefix.trim();
        }

        // 1. 整目录挂载（正常登记，SOYS 自动补 /plugins/MCERP 前缀，与 PREFIX 一致）
        webPage.registerResourceDirectory(plugin, "/", plugin.getClass().getClassLoader(), "dist");

        // 2. index.html 资源路径改写并覆盖
        String html = readResource(plugin, "dist/index.html");
        if (html != null) {
            String rewritten = html
                    .replace("/static/", PREFIX + "/static/")
                    .replace("/favicon.ico", PREFIX + "/favicon.ico");
            webPage.registerPage(plugin, "/index.html", rewritten.getBytes(StandardCharsets.UTF_8), "text/html", true);
            log.info("dist 已托管，入口: " + PREFIX + "/index.html");
        } else {
            log.warn("未找到 dist/index.html，前端托管失败");
        }

        // 3. JS 中 axios baseURL 对齐 SOYS 全局前缀（默认 /api → /api/prod-api）
        int replaced = 0;
        try {
            java.net.URL loc = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            java.io.File jarFile = new java.io.File(loc.toURI());
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry e = entries.nextElement();
                    String name = e.getName();
                    if (e.isDirectory() || !name.startsWith("dist/static/js/") || !name.endsWith(".js")) {
                        continue;
                    }
                    String content = readJarEntry(jar, e);
                    if (content == null) {
                        continue;
                    }
                    boolean needsBaseUrl = content.contains("/prod-api");
                    boolean needsLoginPatch = content.contains("请输入您的密码");
                    if (!needsBaseUrl && !needsLoginPatch) {
                        continue;
                    }
                    String replacedContent = content;
                    if (needsBaseUrl) {
                        replacedContent = replacedContent.replace("/prod-api", apiPrefix + "/prod-api");
                    }
                    if (needsLoginPatch) {
                        // 登录适配：密码框改为非必填（无登录插件提供者时免密码登录，仅用户名+验证码）
                        replacedContent = replacedContent.replace(
                                "{required:!0,trigger:\"blur\",message:\"请输入您的密码\"}",
                                "{required:!1,trigger:\"blur\",message:\"请输入您的密码\"}");
                    }
                    String pagePath = "/static/js/" + name.substring("dist/static/js/".length());
                    webPage.registerPage(plugin, pagePath, replacedContent.getBytes(StandardCharsets.UTF_8),
                            "application/javascript", true);
                    replaced++;
                }
            }
        } catch (Exception ex) {
            log.warn("JS baseURL 改写失败: %s", ex.getMessage());
        }
        if (replaced > 0) {
            log.info("前端 baseURL 已对齐 SOYS 前缀: " + apiPrefix + "/prod-api (" + replaced + " 个 JS 文件)");
        }
    }

    private static String readResource(JavaPlugin plugin, String path) {
        try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return readAll(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static String readJarEntry(JarFile jar, JarEntry e) {
        try (InputStream in = jar.getInputStream(e)) {
            return readAll(in);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
