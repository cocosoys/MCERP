package com.github.cocosoys.mc.mcerp.impl;

import static com.github.cocosoys.mc.mcerp.i18n.McerpI18n.t;

import com.github.cocosoys.mc.mcerp.entity.ErpNotice;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.ErpNoticeService;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 通知公告实现（从 ErpNoticeController 迁入）：CRUD + 首页 Top5。
 */
public class ErpNoticeServiceImpl implements ErpNoticeService {

    private final OperLogService operLog;

    public ErpNoticeServiceImpl(OperLogService operLog) {
        this.operLog = operLog;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String noticeTitle, String noticeType) {
        List<ErpNotice> all = DATA.select(ErpNotice.class);
        List<ErpNotice> filtered = new ArrayList<>();
        for (ErpNotice n : all) {
            if (noticeTitle != null && !noticeTitle.isEmpty() && !n.getNoticeTitle().contains(noticeTitle)) {
                continue;
            }
            if (noticeType != null && !noticeType.isEmpty() && !noticeType.equals(n.getNoticeType())) {
                continue;
            }
            filtered.add(n);
        }
        filtered.sort(Comparator.comparing(ErpNotice::getCreateTime, Comparator.nullsLast(Date::compareTo)).reversed());
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public TableDataInfo listTop() {
        List<ErpNotice> all = DATA.select(ErpNotice.class);
        all.sort(Comparator.comparing(ErpNotice::getCreateTime, Comparator.nullsLast(Date::compareTo)).reversed());
        List<ErpNotice> top = all.size() > 5 ? all.subList(0, 5) : all;
        return TableDataInfo.success(top, top.size());
    }

    @Override
    public AjaxResult detail(String noticeId) {
        ErpNotice n = DATA.get(ErpNotice.class, noticeId);
        if (n == null) {
            return AjaxResult.error(t("mcerp.notice.not-found", "公告不存在"));
        }
        return AjaxResult.success(n);
    }

    @Override
    public AjaxResult add(ErpNotice notice, CredentialPresentation credential) {
        ErpNotice n = new ErpNotice();
        n.setNoticeId(UUID.randomUUID().toString());
        n.setNoticeTitle(notice.getNoticeTitle() == null ? "" : notice.getNoticeTitle());
        n.setNoticeType(notice.getNoticeType() == null || notice.getNoticeType().isEmpty() ? "1" : notice.getNoticeType());
        n.setNoticeContent(notice.getNoticeContent() == null ? "" : notice.getNoticeContent());
        n.setStatus(notice.getStatus() == null || notice.getStatus().isEmpty() ? "0" : notice.getStatus());
        n.setCreateBy("system");
        n.setCreateTime(AuthService.now());
        if (n.getNoticeTitle().isEmpty()) {
            return AjaxResult.error(t("mcerp.notice.title-empty", "公告标题不能为空"));
        }
        DATA.insert(n);
        operLog.record(t("mcerp.operlog.module.notice", "通知公告"), t("mcerp.operlog.action.add-notice", "新增公告"), n.getNoticeTitle(), t("mcerp.common.add-success", "新增成功"));
        return AjaxResult.success(t("mcerp.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(String noticeId, ErpNotice notice) {
        ErpNotice n = noticeId == null || noticeId.isEmpty() ? null : DATA.get(ErpNotice.class, noticeId);
        if (n == null) {
            return AjaxResult.error(t("mcerp.notice.not-found", "公告不存在"));
        }
        if (notice.getNoticeTitle() != null) {
            n.setNoticeTitle(notice.getNoticeTitle());
        }
        if (notice.getNoticeType() != null) {
            n.setNoticeType(notice.getNoticeType());
        }
        if (notice.getNoticeContent() != null) {
            n.setNoticeContent(notice.getNoticeContent());
        }
        if (notice.getStatus() != null) {
            n.setStatus(notice.getStatus());
        }
        DATA.updateById(n);
        operLog.record(t("mcerp.operlog.module.notice", "通知公告"), t("mcerp.operlog.action.edit-notice", "修改公告"), n.getNoticeTitle(), t("mcerp.common.edit-success", "修改成功"));
        return AjaxResult.success(t("mcerp.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return AjaxResult.error(t("mcerp.notice.missing-id", "缺少 noticeId"));
        }
        for (String id : noticeIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            ErpNotice n = DATA.get(ErpNotice.class, id.trim());
            DATA.deleteById(ErpNotice.class, id.trim());
            if (n != null) {
                operLog.record(t("mcerp.operlog.module.notice", "通知公告"), t("mcerp.operlog.action.delete-notice", "删除公告"), n.getNoticeTitle(), t("mcerp.common.delete-success", "删除成功"));
            }
        }
        return AjaxResult.success(t("mcerp.common.delete-success", "删除成功"));
    }

    @Override
    public AjaxResult markRead() {
        return AjaxResult.success();
    }

    @Override
    public AjaxResult markReadAll() {
        return AjaxResult.success();
    }

    @Override
    public TableDataInfo readUsers() {
        return TableDataInfo.success(new ArrayList<>(), 0L);
    }
}
