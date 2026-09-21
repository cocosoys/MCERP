package com.github.cocosoys.mc.mcerp;

/**
 * MCERP 对外门面：其他插件通过 {@code MCERP.getInstance().getApi()} 获取。
 *
 * <p>ERP 模块登记的正轨：继承 {@link McerpExpansion} 并覆写 ERP 声明钩子
 * （displayName/menus()/menusTable()…），一行 {@code register()} 即自动完成
 * 登记——onRegister 写入 ErpRegistry 索引（仅 identifier）、onUnregister 自动摘除；
 * 菜单/路由由 ErpRegistry 实时从 SoysExpansion.REGISTERED 组装，即安即生效。</p>
 */
public interface McerpApi {

    /**
     * ERP 模块索引（投影缓存，仅登记 identifier）。
     */
    ErpRegistry getRegistry();
}
