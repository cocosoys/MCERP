package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 日志导出结果 VO：承载下载文件名、CSV 全文与导出行数。
 *
 * <p>框架注解式 API 的响应体恒为 JSON 信封（{@code WebFrontendHandler} 强制
 * {@code application/json}，无二进制流通道），因此导出由服务端生成 CSV 文本随
 * {@code AjaxResult.data} 返回，前端组装 Blob 后落盘；不使用 {@code Map<String,String>}
 * 拼接返回体，避免契约与序列化口径漂移。</p>
 */
@Data
public class LogExportVO {

    /** 下载文件名（如 {@code operlog_20261123153000.csv}） */
    private String fileName;

    /** CSV 全文（UTF-8，首行为表头，字段按 RFC4180 加引号转义） */
    private String content;

    /** 实际导出的数据行数（不含表头；超过上限时等于上限） */
    private int total;
}
