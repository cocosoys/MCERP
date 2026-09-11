package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.SysOperLog;
import com.github.cocosoys.mc.mcerp.entity.SysUser;
import com.github.cocosoys.mc.mcerp.service.AuthService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户管理（若依契约：经 registerProxyController 代理注册，SOYS 自动补 /api 全局前缀
 * → 实际路由 /api/prod-api/system/user/*，含 @PathVariable 路径参数版本）。
 */
@RequestMapping("/prod-api/system/user")
public class SysUserController {

    private final AuthService auth;

    public SysUserController(AuthService auth) {
        this.auth = auth;
    }

    @ApiName("用户列表")
    @ApiPermission("system:user:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "userName", required = false) String userName,
                              @RequestParam(name = "status", required = false) String status,
                              @RequestParam(name = "phonenumber", required = false) String phonenumber) {
        List<SysUser> all = Store.select(SysUser.class);
        List<SysUser> filtered = new ArrayList<>();
        for (SysUser u : all) {
            if (userName != null && !userName.isEmpty() && !contains(u.getUserName(), userName)) {
                continue;
            }
            if (status != null && !status.isEmpty() && !status.equals(u.getStatus())) {
                continue;
            }
            if (phonenumber != null && !phonenumber.isEmpty() && !contains(u.getPhonenumber(), phonenumber)) {
                continue;
            }
            filtered.add(u);
        }
        filtered.sort(Comparator.comparing(SysUser::getCreateTime, Comparator.nullsLast(String::compareTo)).reversed());
        return page(filtered, pageNum, pageSize);
    }

    @ApiName("用户详情")
    @ApiPermission("system:user:query")
    @GetMapping("/{userId}")
    public AjaxResult detail(@PathVariable(name = "userId") String userId) {
        SysUser u = userId == null ? null : Store.get(SysUser.class, userId);
        if (u == null) {
            return AjaxResult.error("用户不存在");
        }
        return AjaxResult.success(u);
    }

    @ApiName("新增用户")
    @ApiPermission("system:user:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        String userName = Json.getString(map, "userName", "");
        if (userName.isEmpty()) {
            return AjaxResult.error("用户名不能为空");
        }
        if (auth.findUser(userName) != null) {
            return AjaxResult.error("用户名已存在");
        }
        SysUser u = new SysUser();
        u.setUserId(UUID.randomUUID().toString());
        u.setUserName(userName);
        u.setNickName(Json.getString(map, "nickName", userName));
        u.setEmail(Json.getString(map, "email", ""));
        u.setPhonenumber(Json.getString(map, "phonenumber", ""));
        u.setSex(Json.getString(map, "sex", "0"));
        u.setStatus(Json.getString(map, "status", "0"));
        u.setRemark(Json.getString(map, "remark", ""));
        u.setCreateTime(AuthService.now());
        Store.insert(u);
        recordOper("用户管理", "新增用户", userName, "新增成功");
        return AjaxResult.success("新增成功");
    }

    @ApiName("编辑用户")
    @ApiPermission("system:user:edit")
    @PutMapping("/{userId}")
    public AjaxResult update(@PathVariable(name = "userId") String userId, @RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        SysUser u = userId == null || userId.isEmpty() ? null : Store.get(SysUser.class, userId);
        if (u == null) {
            return AjaxResult.error("用户不存在");
        }
        u.setNickName(Json.getString(map, "nickName", u.getNickName()));
        u.setEmail(Json.getString(map, "email", u.getEmail()));
        u.setPhonenumber(Json.getString(map, "phonenumber", u.getPhonenumber()));
        u.setSex(Json.getString(map, "sex", u.getSex()));
        u.setStatus(Json.getString(map, "status", u.getStatus()));
        u.setRemark(Json.getString(map, "remark", u.getRemark()));
        Store.updateById(u);
        recordOper("用户管理", "修改用户", u.getUserName(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @ApiName("删除用户")
    @ApiPermission("system:user:remove")
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable(name = "userIds") String userIds,
                             CredentialPresentation credential) {
        if (userIds == null || userIds.isEmpty()) {
            return AjaxResult.error("缺少 userId");
        }
        String current = auth.currentPlayer(credential);
        for (String id : userIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysUser u = Store.get(SysUser.class, id.trim());
            if (u != null && current != null && u.getUserName().equalsIgnoreCase(current)) {
                return AjaxResult.error("不能删除当前登录账号");
            }
            Store.deleteById(SysUser.class, id.trim());
            recordOper("用户管理", "删除用户", u == null ? id.trim() : u.getUserName(), "删除成功");
        }
        return AjaxResult.success("删除成功");
    }

    @ApiName("用户状态修改")
    @ApiPermission("system:user:changeStatus")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        String userId = Json.getString(map, "userId", "");
        SysUser u = userId.isEmpty() ? null : Store.get(SysUser.class, userId);
        if (u == null) {
            return AjaxResult.error("用户不存在");
        }
        u.setStatus(Json.getString(map, "status", "0"));
        Store.updateById(u);
        recordOper("用户管理", "修改用户状态", u.getUserName(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @ApiName("重置密码")
    @ApiPermission("system:user:resetPwd")
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody String body) {
        // 密码完全由 AuthMe 管理（游戏内 /changepassword 或 SOYS 登录桥）
        return AjaxResult.error("密码由游戏内 AuthMe 管理，请在游戏中使用 /changepassword 修改");
    }

    @ApiName("部门树")
    @ApiPermission("system:user:list")
    @GetMapping("/deptTree")
    public AjaxResult deptTree() {
        return AjaxResult.success(new ArrayList<>()); // 未实现部门模块
    }

    @ApiName("用户角色")
    @ApiPermission("system:user:query")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable(name = "userId") String userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roles", new ArrayList<>()); // 角色模块未实现，返回空
        data.put("user", Store.get(SysUser.class, userId));
        return AjaxResult.success(data);
    }

    @ApiName("用户角色保存")
    @ApiPermission("system:user:edit")
    @PutMapping("/authRole")
    public AjaxResult authRoleSave(@RequestBody String body) {
        return AjaxResult.success("角色模块未实现，操作跳过");
    }

    @ApiName("个人中心")
    @GetMapping("/profile")
    public AjaxResult profile(CredentialPresentation credential) {
        String player = auth.currentPlayer(credential);
        if (player == null) {
            return AjaxResult.error("未登录");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> info = auth.getInfo(credential);
        Object user = info.get("data") instanceof Map ? ((Map<?, ?>) info.get("data")).get("user") : null;
        data.put("user", user);
        data.put("roleGroup", auth.isOp(player) ? "超级管理员" : "普通玩家");
        data.put("postGroup", "ERP 后台");
        return AjaxResult.success(data);
    }

    @ApiName("修改密码")
    @PutMapping("/profile/updatePwd")
    public AjaxResult updatePwd(@RequestBody String body) {
        return AjaxResult.error("密码由游戏内 AuthMe 管理，请在游戏中使用 /changepassword 修改");
    }

    @ApiName("头像上传")
    @PostMapping("/profile/avatar")
    public AjaxResult avatar(@RequestBody String body) {
        return AjaxResult.error("头像上传暂未启用（SOYS 上传接口未接入）");
    }

    // ===== 内部 =====

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }

    static TableDataInfo page(List<?> rows, Integer pageNum, Integer pageSize) {
        if (rows == null || rows.isEmpty()) {
            return TableDataInfo.success(new ArrayList<>(), 0L);
        }
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int total = rows.size();
        int from = (pn - 1) * ps;
        int to = Math.min(from + ps, total);
        List<Object> page = new ArrayList<>();
        if (from < total) {
            page.addAll(rows.subList(from, to));
        }
        return TableDataInfo.success(page, total);
    }

    static void recordOper(String title, String business, String operName, String result) {
        try {
            SysOperLog log = new SysOperLog();
            log.setOperId(UUID.randomUUID().toString());
            log.setTitle(title);
            log.setBusinessType("新增".equals(business) ? 1 : "修改".equals(business) ? 2 : 3);
            log.setMethod(business);
            log.setRequestMethod("");
            log.setOperName(operName == null ? "" : operName);
            log.setOperUrl("");
            log.setOperIp("");
            log.setOperParam("");
            log.setJsonResult("");
            log.setStatus(0);
            log.setErrorMsg("");
            log.setOperTime(AuthService.now());
            Store.insert(log);
        } catch (Exception ignored) {
        }
    }
}
