package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.SysNotice;
import com.github.cocosoys.mc.mcerp.service.Store;
import com.github.cocosoys.mc.mcerp.util.Json;
import com.github.cocosoys.mc.mcerp.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 通知公告（若依契约：经 registerProxyController 代理注册，SOYS 自动补 /api 全局前缀
 * → 实际路由 /api/prod-api/system/notice/*）。
 */
@RequestMapping("/prod-api/system/notice")
public class SysNoticeController {

    public SysNoticeController() {
    }

    @ApiName("公告列表")
    @ApiPermission("system:notice:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "noticeTitle", required = false) String noticeTitle,
                              @RequestParam(name = "noticeType", required = false) String noticeType) {
        List<SysNotice> all = Store.select(SysNotice.class);
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
        return SysUserController.page(filtered, pageNum, pageSize);
    }

    @ApiName("首页公告")
    @GetMapping("/listTop")
    public TableDataInfo listTop() {
        List<SysNotice> all = Store.select(SysNotice.class);
        all.sort(Comparator.comparing(SysNotice::getCreateTime, Comparator.nullsLast(String::compareTo)).reversed());
        List<SysNotice> top = all.size() > 5 ? all.subList(0, 5) : all;
        return TableDataInfo.success(top, top.size());
    }

    @ApiName("公告详情")
    @ApiPermission("system:notice:query")
    @GetMapping("/{noticeId}")
    public AjaxResult detail(@PathVariable(name = "noticeId") String noticeId) {
        SysNotice n = Store.get(SysNotice.class, noticeId);
        if (n == null) {
            return AjaxResult.error("公告不存在");
        }
        return AjaxResult.success(n);
    }

    @ApiName("新增公告")
    @ApiPermission("system:notice:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody String body, CredentialPresentation credential) {
        Map<String, Object> map = Json.parseObject(body);
        SysNotice n = new SysNotice();
        n.setNoticeId(UUID.randomUUID().toString());
        n.setNoticeTitle(Json.getString(map, "noticeTitle", ""));
        n.setNoticeType(Json.getString(map, "noticeType", "1"));
        n.setNoticeContent(Json.getString(map, "noticeContent", ""));
        n.setStatus(Json.getString(map, "status", "0"));
        n.setCreateBy("system");
        n.setCreateTime(com.github.cocosoys.mc.mcerp.service.AuthService.now());
        if (n.getNoticeTitle().isEmpty()) {
            return AjaxResult.error("公告标题不能为空");
        }
        Store.insert(n);
        SysUserController.recordOper("通知公告", "新增公告", n.getNoticeTitle(), "新增成功");
        return AjaxResult.success("新增成功");
    }

    @ApiName("编辑公告")
    @ApiPermission("system:notice:edit")
    @PutMapping("/{noticeId}")
    public AjaxResult update(@PathVariable(name = "noticeId") String noticeId, @RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        SysNotice n = noticeId == null || noticeId.isEmpty() ? null : Store.get(SysNotice.class, noticeId);
        if (n == null) {
            return AjaxResult.error("公告不存在");
        }
        n.setNoticeTitle(Json.getString(map, "noticeTitle", n.getNoticeTitle()));
        n.setNoticeType(Json.getString(map, "noticeType", n.getNoticeType()));
        n.setNoticeContent(Json.getString(map, "noticeContent", n.getNoticeContent()));
        n.setStatus(Json.getString(map, "status", n.getStatus()));
        Store.updateById(n);
        SysUserController.recordOper("通知公告", "修改公告", n.getNoticeTitle(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @ApiName("删除公告")
    @ApiPermission("system:notice:remove")
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable(name = "noticeIds") String noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return AjaxResult.error("缺少 noticeId");
        }
        for (String id : noticeIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysNotice n = Store.get(SysNotice.class, id.trim());
            Store.deleteById(SysNotice.class, id.trim());
            if (n != null) {
                SysUserController.recordOper("通知公告", "删除公告", n.getNoticeTitle(), "删除成功");
            }
        }
        return AjaxResult.success("删除成功");
    }

    @ApiName("标记已读")
    @PostMapping("/markRead")
    public AjaxResult markRead(@RequestBody String body) {
        return AjaxResult.success();
    }

    @ApiName("全部已读")
    @PostMapping("/markReadAll")
    public AjaxResult markReadAll(@RequestBody String body) {
        return AjaxResult.success();
    }

    @ApiName("已读用户")
    @ApiPermission("system:notice:list")
    @GetMapping("/readUsers/list")
    public TableDataInfo readUsers() {
        return TableDataInfo.success(new ArrayList<>(), 0L);
    }
}
