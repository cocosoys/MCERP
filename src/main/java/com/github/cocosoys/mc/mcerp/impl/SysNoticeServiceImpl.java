package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.entity.SysNotice;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.SysNoticeService;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 通知公告实现（从 SysNoticeController 迁入）：CRUD + 首页 Top5。
 */
public class SysNoticeServiceImpl implements SysNoticeService {

    private final OperLogService operLog;

    public SysNoticeServiceImpl(OperLogService operLog) {
        this.operLog = operLog;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String noticeTitle, String noticeType) {
        List<SysNotice> all = DATA.select(SysNotice.class);
        List<SysNotice> filtered = new ArrayList<>();
        for (SysNotice n : all) {
            if (noticeTitle != null && !noticeTitle.isEmpty() && !n.getNoticeTitle().contains(noticeTitle)) {
                continue;
            }
            if (noticeType != null && !noticeType.isEmpty() && !noticeType.equals(n.getNoticeType())) {
                continue;
            }
            filtered.add(n);
        }
        filtered.sort(Comparator.comparing(SysNotice::getCreateTime, Comparator.nullsLast(String::compareTo)).reversed());
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public TableDataInfo listTop() {
        List<SysNotice> all = DATA.select(SysNotice.class);
        all.sort(Comparator.comparing(SysNotice::getCreateTime, Comparator.nullsLast(String::compareTo)).reversed());
        List<SysNotice> top = all.size() > 5 ? all.subList(0, 5) : all;
        return TableDataInfo.success(top, top.size());
    }

    @Override
    public AjaxResult detail(String noticeId) {
        SysNotice n = DATA.get(SysNotice.class, noticeId);
        if (n == null) {
            return AjaxResult.error("公告不存在");
        }
        return AjaxResult.success(n);
    }

    @Override
    public AjaxResult add(SysNotice notice, CredentialPresentation credential) {
        SysNotice n = new SysNotice();
        n.setNoticeId(UUID.randomUUID().toString());
        n.setNoticeTitle(notice.getNoticeTitle() == null ? "" : notice.getNoticeTitle());
        n.setNoticeType(notice.getNoticeType() == null || notice.getNoticeType().isEmpty() ? "1" : notice.getNoticeType());
        n.setNoticeContent(notice.getNoticeContent() == null ? "" : notice.getNoticeContent());
        n.setStatus(notice.getStatus() == null || notice.getStatus().isEmpty() ? "0" : notice.getStatus());
        n.setCreateBy("system");
        n.setCreateTime(AuthService.now());
        if (n.getNoticeTitle().isEmpty()) {
            return AjaxResult.error("公告标题不能为空");
        }
        DATA.insert(n);
        operLog.record("通知公告", "新增公告", n.getNoticeTitle(), "新增成功");
        return AjaxResult.success("新增成功");
    }

    @Override
    public AjaxResult update(String noticeId, SysNotice notice) {
        SysNotice n = noticeId == null || noticeId.isEmpty() ? null : DATA.get(SysNotice.class, noticeId);
        if (n == null) {
            return AjaxResult.error("公告不存在");
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
        operLog.record("通知公告", "修改公告", n.getNoticeTitle(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @Override
    public AjaxResult remove(String noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return AjaxResult.error("缺少 noticeId");
        }
        for (String id : noticeIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysNotice n = DATA.get(SysNotice.class, id.trim());
            DATA.deleteById(SysNotice.class, id.trim());
            if (n != null) {
                operLog.record("通知公告", "删除公告", n.getNoticeTitle(), "删除成功");
            }
        }
        return AjaxResult.success("删除成功");
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
