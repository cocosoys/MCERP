package com.github.cocosoys.mc.mcerp.impl;

import static com.github.cocosoys.mc.mcerp.i18n.McerpI18n.t;

import com.github.cocosoys.mc.mcerp.entity.ErpLogininfor;
import com.github.cocosoys.mc.mcerp.entity.ErpOperLog;
import com.github.cocosoys.mc.mcerp.entity.vo.LogExportVO;
import com.github.cocosoys.mc.mcerp.enums.BusinessType;
import com.github.cocosoys.mc.mcerp.service.ErpLogService;
import com.github.cocosoys.mc.mcerp.util.Ids;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 日志管理实现（从 ErpLogController 迁入）：操作/登录日志列表、导出、删除、清空。
 *
 * <p>导出与列表复用同一筛选/排序路径（{@link #filterOperlog} / {@link #filterLogininfor}），
 * 保证页面筛选条件与导出内容口径一致；CSV 文本随 JSON 信封返回，由前端落盘
 * （框架注解式 API 无二进制流通道，详见 {@link LogExportVO}）。</p>
 */
public class ErpLogServiceImpl implements ErpLogService {

    /** 单次导出行数上限：日志表可能很大，避免一次性塞入过大的 JSON 响应体 */
    private static final int MAX_EXPORT_ROWS = 10000;

    // ===== 操作日志 =====

    @Override
    public TableDataInfo operlogList(Integer pageNum, Integer pageSize, String title, String operName) {
        return PageUtils.page(filterOperlog(title, operName), pageNum, pageSize);
    }

    @Override
    public AjaxResult operlogExport(String title, String operName) {
        List<ErpOperLog> rows = filterOperlog(title, operName);
        int total = Math.min(rows.size(), MAX_EXPORT_ROWS);

        StringBuilder sb = new StringBuilder();
        sb.append(t("mcerp.export.operlog.header",
                "序号,操作模块,业务类型,请求方式,操作人员,操作地址,操作状态,操作时间")).append("\r\n");
        int seq = 0;
        for (int i = 0; i < total; i++) {
            ErpOperLog l = rows.get(i);
            sb.append(csv(String.valueOf(++seq))).append(',')
                    .append(csv(l.getTitle())).append(',')
                    .append(csv(t("mcerp.biz." + l.getBusinessType(),
                            BusinessType.fromCode(l.getBusinessType()).getLabel()))).append(',')
                    .append(csv(l.getRequestMethod())).append(',')
                    .append(csv(l.getOperName())).append(',')
                    .append(csv(l.getOperIp())).append(',')
                    .append(csv(statusLabel(l.getStatus()))).append(',')
                    .append(csv(formatDate(l.getOperTime()))).append("\r\n");
        }
        return AjaxResult.success(buildVo("operlog", sb, total));
    }

    @Override
    public AjaxResult operlogRemove(String operIds) {
        if (operIds == null || operIds.isEmpty()) {
            return AjaxResult.error(t("mcerp.log.missing-operId", "缺少 operId"));
        }
        for (String id : operIds.split(",")) {
            if (!id.trim().isEmpty()) {
                DATA.deleteById(ErpOperLog.class, Ids.parse(id));
            }
        }
        return AjaxResult.success(t("mcerp.common.delete-success", "删除成功"));
    }

    @Override
    public AjaxResult operlogClean() {
        for (ErpOperLog l : DATA.select(ErpOperLog.class)) {
            DATA.deleteById(ErpOperLog.class, l.getOperId());
        }
        return AjaxResult.success(t("mcerp.common.clear-success", "清空成功"));
    }

    // ===== 登录日志 =====

    @Override
    public TableDataInfo logininforList(Integer pageNum, Integer pageSize, String userName, String status) {
        return PageUtils.page(filterLogininfor(userName, status), pageNum, pageSize);
    }

    @Override
    public AjaxResult logininforExport(String userName, String status) {
        List<ErpLogininfor> rows = filterLogininfor(userName, status);
        int total = Math.min(rows.size(), MAX_EXPORT_ROWS);

        StringBuilder sb = new StringBuilder();
        sb.append(t("mcerp.export.logininfor.header",
                "序号,用户账号,登录IP,登录状态,提示消息,登录时间")).append("\r\n");
        int seq = 0;
        for (int i = 0; i < total; i++) {
            ErpLogininfor l = rows.get(i);
            sb.append(csv(String.valueOf(++seq))).append(',')
                    .append(csv(l.getUserName())).append(',')
                    .append(csv(l.getIpaddr())).append(',')
                    .append(csv(statusLabel(l.getStatus()))).append(',')
                    .append(csv(l.getMsg())).append(',')
                    .append(csv(formatDate(l.getLoginTime()))).append("\r\n");
        }
        return AjaxResult.success(buildVo("logininfor", sb, total));
    }

    @Override
    public AjaxResult logininforRemove(String infoIds) {
        if (infoIds == null || infoIds.isEmpty()) {
            return AjaxResult.error(t("mcerp.log.missing-infoId", "缺少 infoId"));
        }
        for (String id : infoIds.split(",")) {
            if (!id.trim().isEmpty()) {
                DATA.deleteById(ErpLogininfor.class, Ids.parse(id));
            }
        }
        return AjaxResult.success(t("mcerp.common.delete-success", "删除成功"));
    }

    @Override
    public AjaxResult logininforClean() {
        for (ErpLogininfor l : DATA.select(ErpLogininfor.class)) {
            DATA.deleteById(ErpLogininfor.class, l.getInfoId());
        }
        return AjaxResult.success(t("mcerp.common.clear-success", "清空成功"));
    }

    @Override
    public AjaxResult unlock(String userName) {
        return AjaxResult.success(t("mcerp.log.unlock-success", "解锁成功（由 AuthMe 管理）"));
    }

    // ===== 内部 =====

    /**
     * 操作日志筛选 + 排序（列表与导出共用）：title/operName 模糊命中，操作时间倒序。
     *
     * @param title    模块标题模糊条件（可空）
     * @param operName 操作人员模糊条件（可空）
     * @return 过滤并排序后的日志列表（非 null）
     */
    private static List<ErpOperLog> filterOperlog(String title, String operName) {
        List<ErpOperLog> all = DATA.select(ErpOperLog.class);
        List<ErpOperLog> filtered = new ArrayList<>();
        for (ErpOperLog l : all) {
            if (title != null && !title.isEmpty() && !contains(l.getTitle(), title)) {
                continue;
            }
            if (operName != null && !operName.isEmpty() && !contains(l.getOperName(), operName)) {
                continue;
            }
            filtered.add(l);
        }
        filtered.sort(Comparator.comparing(ErpOperLog::getOperTime, Comparator.nullsLast(Date::compareTo)).reversed());
        return filtered;
    }

    /**
     * 登录日志筛选 + 排序（列表与导出共用）：userName 模糊、status 精确，登录时间倒序。
     *
     * @param userName 用户账号模糊条件（可空）
     * @param status   登录状态（0 成功 / 1 失败，可空）
     * @return 过滤并排序后的日志列表（非 null）
     */
    private static List<ErpLogininfor> filterLogininfor(String userName, String status) {
        List<ErpLogininfor> all = DATA.select(ErpLogininfor.class);
        List<ErpLogininfor> filtered = new ArrayList<>();
        for (ErpLogininfor l : all) {
            if (userName != null && !userName.isEmpty() && !contains(l.getUserName(), userName)) {
                continue;
            }
            if (status != null && !status.isEmpty() && !status.equals(l.getStatus())) {
                continue;
            }
            filtered.add(l);
        }
        filtered.sort(Comparator.comparing(ErpLogininfor::getLoginTime, Comparator.nullsLast(Date::compareTo)).reversed());
        return filtered;
    }

    /**
     * 组装导出结果 VO：文件名按 {@code <prefix>_yyyyMMddHHmmss.csv} 生成。
     *
     * @param prefix 文件名前缀（operlog / logininfor）
     * @param sb     CSV 全文
     * @param total  数据行数
     * @return 导出结果 VO
     */
    private static LogExportVO buildVo(String prefix, StringBuilder sb, int total) {
        LogExportVO vo = new LogExportVO();
        vo.setFileName(prefix + "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".csv");
        vo.setContent(sb.toString());
        vo.setTotal(total);
        return vo;
    }

    /**
     * 状态码转展示文案：0 成功 / 1 失败（运营日志与登录日志共用同一状态口径）。
     *
     * @param status 状态码（操作日志为 int，登录日志为字符串，统一按数字解析）
     * @return 本地化文案
     */
    private static String statusLabel(int status) {
        return status == 0
                ? t("mcerp.export.status.success", "成功")
                : t("mcerp.export.status.fail", "失败");
    }

    /**
     * 状态字符串转展示文案：非 "0" 一律按失败处理（登录日志 status 为字符串字段）。
     *
     * @param status 状态字符串（"0" 成功 / 其它失败，可为 null）
     * @return 本地化文案
     */
    private static String statusLabel(String status) {
        return "0".equals(status)
                ? t("mcerp.export.status.success", "成功")
                : t("mcerp.export.status.fail", "失败");
    }

    /**
     * 日期转 {@code yyyy-MM-dd HH:mm:ss} 文本；null 输出空串。
     *
     * @param date 时间
     * @return 格式化文本
     */
    private static String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    /**
     * CSV 字段转义（RFC4180）：整体加双引号，内部双引号翻倍；null 输出空字段。
     *
     * @param value 原始值
     * @return 可直接拼接的 CSV 字段
     */
    private static String csv(String value) {
        String v = value == null ? "" : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
