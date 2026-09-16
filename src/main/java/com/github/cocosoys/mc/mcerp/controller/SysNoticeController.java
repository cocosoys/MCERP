package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.SysNotice;
import com.github.cocosoys.mc.mcerp.service.SysNoticeService;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPublic;
import com.github.cocosoys.mc.soyshttpovermc.annotations.DeleteMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 通知公告（若依契约）：路由 /api/plugins/MCERP/system/notice/*。
 * CRUD 全部委托 {@link SysNoticeService}。
 */
@RequestMapping("/system/notice")
public class SysNoticeController {

    private final SysNoticeService noticeService;

    public SysNoticeController(SysNoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @ApiName("公告列表")
    @ApiPermission("system:notice:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "noticeTitle", required = false) String noticeTitle,
                              @RequestParam(name = "noticeType", required = false) String noticeType) {
        return noticeService.list(pageNum, pageSize, noticeTitle, noticeType);
    }

    @ApiName("首页公告")
    @ApiPublic // 网关 auth 已保护 /api/*（无凭证 401），此处仅解除注册表默认拒绝 → 登录即可
    @GetMapping("/listTop")
    public TableDataInfo listTop() {
        return noticeService.listTop();
    }

    @ApiName("公告详情")
    @ApiPermission("system:notice:query")
    @GetMapping("/{noticeId}")
    public AjaxResult detail(@PathVariable(name = "noticeId") String noticeId) {
        return noticeService.detail(noticeId);
    }

    @ApiName("新增公告")
    @ApiPermission("system:notice:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody SysNotice notice, CredentialPresentation credential) {
        return noticeService.add(notice, credential);
    }

    @ApiName("编辑公告")
    @ApiPermission("system:notice:edit")
    @PutMapping("/{noticeId}")
    public AjaxResult update(@PathVariable(name = "noticeId") String noticeId, @RequestBody SysNotice notice) {
        return noticeService.update(noticeId, notice);
    }

    @ApiName("删除公告")
    @ApiPermission("system:notice:remove")
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable(name = "noticeIds") String noticeIds) {
        return noticeService.remove(noticeIds);
    }

    @ApiName("标记已读")
    @ApiPublic // 网关 401 兜底，登录即可（已读状态当前仅记账占位）
    @PostMapping("/markRead")
    public AjaxResult markRead() {
        return noticeService.markRead();
    }

    @ApiName("全部已读")
    @ApiPublic // 网关 401 兜底，登录即可（已读状态当前仅记账占位）
    @PostMapping("/markReadAll")
    public AjaxResult markReadAll() {
        return noticeService.markReadAll();
    }

    @ApiName("已读用户")
    @ApiPermission("system:notice:list")
    @GetMapping("/readUsers/list")
    public TableDataInfo readUsers() {
        return noticeService.readUsers();
    }
}
