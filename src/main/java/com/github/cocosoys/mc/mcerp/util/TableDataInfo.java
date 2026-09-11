package com.github.cocosoys.mc.mcerp.util;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 若依分页返回体：{code, msg, rows, total}。前端 axios 拦截器按 rows/total 渲染表格。
 */
public class TableDataInfo extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public static TableDataInfo success(List<?> rows, long total) {
        TableDataInfo t = new TableDataInfo();
        t.put("code", 200);
        t.put("msg", "查询成功");
        t.put("rows", rows == null ? new java.util.ArrayList<>() : rows);
        t.put("total", total);
        return t;
    }

    public static TableDataInfo error(String msg) {
        TableDataInfo t = new TableDataInfo();
        t.put("code", 500);
        t.put("msg", msg);
        t.put("rows", new java.util.ArrayList<>());
        t.put("total", 0L);
        return t;
    }
}
