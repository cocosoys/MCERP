package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.soyshttpovermc.orm.SQL;
import com.github.cocosoys.mc.soyshttpovermc.orm.YAML;

import java.util.List;

/**
 * 双存储路由（SQL/YAML 双兼容）：SOYS 配置了 mysql/sqlite 存储时走 SQL，
 * 否则回退 YAML 文件存储（data/&lt;表名&gt;.yml）。对业务层透明。
 */
public final class Store {

    private Store() {
    }

    public static boolean sqlEnabled() {
        return SQL.Pojo.isAvailable();
    }

    public static <T> List<T> select(Class<T> beanClass) {
        return sqlEnabled() ? SQL.Pojo.select(beanClass) : YAML.Pojo.select(beanClass);
    }

    public static <T> T get(Class<T> beanClass, Object id) {
        return sqlEnabled() ? SQL.Pojo.get(beanClass, id) : YAML.Pojo.get(beanClass, id);
    }

    public static <T> boolean insert(T bean) {
        return sqlEnabled() ? SQL.Pojo.insert(bean) : YAML.Pojo.insert(bean);
    }

    public static <T> boolean updateById(T bean) {
        return sqlEnabled() ? SQL.Pojo.updateById(bean) : YAML.Pojo.updateById(bean);
    }

    public static <T> boolean deleteById(Class<T> beanClass, Object id) {
        return sqlEnabled() ? SQL.Pojo.deleteById(beanClass, id) : YAML.Pojo.deleteById(beanClass, id);
    }
}
