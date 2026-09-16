package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.entity.SysNotice;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 通知公告服务（抽象契约）：公告 CRUD、首页 Top5。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.SysNoticeServiceImpl}。
 */
public interface SysNoticeService {

    TableDataInfo list(Integer pageNum, Integer pageSize, String noticeTitle, String noticeType);

    TableDataInfo listTop();

    AjaxResult detail(String noticeId);

    AjaxResult add(SysNotice notice, CredentialPresentation credential);

    AjaxResult update(String noticeId, SysNotice notice);

    AjaxResult remove(String noticeIds);

    AjaxResult markRead();

    AjaxResult markReadAll();

    TableDataInfo readUsers();
}
