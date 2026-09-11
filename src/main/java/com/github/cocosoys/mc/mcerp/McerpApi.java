package com.github.cocosoys.mc.mcerp;

/**
 * MCERP 对外门面：其他插件通过 {@code MCERP.getInstance().getApi()} 获取，
 * 在自身 onEnable 中调用 {@code getRegistry().registerModule(module)} 完成登记。
 */
public interface McerpApi {

    /**
     * ERP 模块注册中心。
     */
    EripRegistry getRegistry();
}
