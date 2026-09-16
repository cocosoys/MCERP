package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.EripRegistry;
import com.github.cocosoys.mc.mcerp.entity.SysMenu;
import com.github.cocosoys.mc.mcerp.entity.SysUser;
import com.github.cocosoys.mc.mcerp.entity.vo.MenuTreeVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ProfileVO;
import com.github.cocosoys.mc.mcerp.entity.vo.RoleOptionVO;
import com.github.cocosoys.mc.mcerp.entity.vo.UserAuthRoleVO;
import com.github.cocosoys.mc.mcerp.entity.vo.UserDetailVO;
import com.github.cocosoys.mc.mcerp.entity.vo.AuthRoleSaveVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ChangeStatusVO;
import com.github.cocosoys.mc.mcerp.entity.vo.SavePermsVO;
import com.github.cocosoys.mc.mcerp.entity.vo.UserPermsVO;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.SysUserService;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.permission.local.LocalPermissionStore;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.SoysPermGroup;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.SoysPermPermission;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 用户管理实现（从 SysUserController 迁入）：CRUD、状态、角色/权限同步。
 * 角色（权限组）与权限节点（USER 类型）的增删同步在此编排，保证多步一致。
 */
public class SysUserServiceImpl implements SysUserService {

    /** 数据权限节点（RuoYi 数据范围语义的权限化表达；当前权限分配仅限菜单 perms，暂不使用）。 */
    // private static final String DATA_PERM_ALL = "mcerp:data:all";

    private final AuthService auth;
    private final OperLogService operLog;

    public SysUserServiceImpl(AuthService auth, OperLogService operLog) {
        this.auth = auth;
        this.operLog = operLog;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String userName, String status, String phonenumber) {
        List<SysUser> all = DATA.select(SysUser.class);
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
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult formInit(CredentialPresentation credential) {
        AjaxResult ok = AjaxResult.success();
        // 当前登录者（新增表单顶部展示 / 创建人）
        Map<String, Object> user = new LinkedHashMap<>();
        String player = auth.currentPlayer(credential);
        user.put("userId", player == null ? "" : player);
        user.put("userName", player == null ? "" : player);
        user.put("nickName", player == null ? "" : player);
        ok.put("user", user);
        // 角色下拉（SOYS 权限组）
        ok.put("roles", roleOptions());
        // 岗位：MCERP 未实现岗位模块 → 空列表
        ok.put("posts", new ArrayList<>());
        return ok;
    }

    @Override
    public AjaxResult detail(String userId) {
        SysUser u = userId == null ? null : DATA.get(SysUser.class, userId);
        if (u == null) {
            return AjaxResult.error("用户不存在");
        }
        UserDetailVO vo = new UserDetailVO();
        vo.setUserId(u.getUserId());
        vo.setUserName(u.getUserName());
        vo.setNickName(u.getNickName());
        vo.setEmail(u.getEmail());
        vo.setPhonenumber(u.getPhonenumber());
        vo.setSex(u.getSex());
        vo.setStatus(u.getStatus());
        vo.setRemark(u.getRemark());
        vo.setCreateTime(u.getCreateTime());
        vo.setRoles(roleOptions());
        vo.setRoleIds(userRoleIds(u.getUserName()));
        vo.setPostIds(new ArrayList<>()); // 岗位模块未实现
        return AjaxResult.success(vo);
    }

    @Override
    public AjaxResult add(SysUser user) {
        String userName = user.getUserName() == null ? "" : user.getUserName().trim();
        if (userName.isEmpty()) {
            return AjaxResult.error("用户名不能为空");
        }
        if (auth.findUser(userName) != null) {
            return AjaxResult.error("用户名已存在");
        }
        SysUser u = new SysUser();
        u.setUserId(UUID.randomUUID().toString());
        u.setUserName(userName);
        u.setNickName(user.getNickName() == null || user.getNickName().isEmpty() ? userName : user.getNickName());
        u.setEmail(user.getEmail() == null ? "" : user.getEmail());
        u.setPhonenumber(user.getPhonenumber() == null ? "" : user.getPhonenumber());
        u.setSex(user.getSex() == null || user.getSex().isEmpty() ? "0" : user.getSex());
        u.setStatus(user.getStatus() == null || user.getStatus().isEmpty() ? "0" : user.getStatus());
        u.setRemark(user.getRemark() == null ? "" : user.getRemark());
        u.setCreateTime(AuthService.now());
        DATA.insert(u);
        operLog.record("用户管理", "新增用户", userName, "新增成功");
        return AjaxResult.success("新增成功");
    }

    @Override
    public AjaxResult update(String userId, SysUser user) {
        SysUser u = userId == null || userId.isEmpty() ? null : DATA.get(SysUser.class, userId);
        if (u == null) {
            return AjaxResult.error("用户不存在");
        }
        if (user.getNickName() != null) {
            u.setNickName(user.getNickName());
        }
        if (user.getEmail() != null) {
            u.setEmail(user.getEmail());
        }
        if (user.getPhonenumber() != null) {
            u.setPhonenumber(user.getPhonenumber());
        }
        if (user.getSex() != null) {
            u.setSex(user.getSex());
        }
        if (user.getStatus() != null) {
            u.setStatus(user.getStatus());
        }
        if (user.getRemark() != null) {
            u.setRemark(user.getRemark());
        }
        DATA.updateById(u);
        operLog.record("用户管理", "修改用户", u.getUserName(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @Override
    public AjaxResult remove(String userIds, CredentialPresentation credential) {
        if (userIds == null || userIds.isEmpty()) {
            return AjaxResult.error("缺少 userId");
        }
        String current = auth.currentPlayer(credential);
        for (String id : userIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysUser u = DATA.get(SysUser.class, id.trim());
            if (u != null && current != null && u.getUserName().equalsIgnoreCase(current)) {
                return AjaxResult.error("不能删除当前登录账号");
            }
            DATA.deleteById(SysUser.class, id.trim());
            operLog.record("用户管理", "删除用户", u == null ? id.trim() : u.getUserName(), "删除成功");
        }
        return AjaxResult.success("删除成功");
    }

    @Override
    public AjaxResult changeStatus(ChangeStatusVO vo) {
        String userId = vo.getUserId() == null ? "" : vo.getUserId();
        SysUser u = userId.isEmpty() ? null : DATA.get(SysUser.class, userId);
        if (u == null) {
            return AjaxResult.error("用户不存在");
        }
        u.setStatus(vo.getStatus() == null || vo.getStatus().isEmpty() ? "0" : vo.getStatus());
        DATA.updateById(u);
        operLog.record("用户管理", "修改用户状态", u.getUserName(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @Override
    public AjaxResult resetPwd() {
        // 密码完全由 AuthMe 管理（游戏内 /changepassword 或 SOYS 登录桥）
        return AjaxResult.error("密码由游戏内 AuthMe 管理，请在游戏中使用 /changepassword 修改");
    }

    @Override
    public AjaxResult deptTree() {
        return AjaxResult.success(new ArrayList<>()); // 未实现部门模块
    }

    @Override
    public AjaxResult authRole(String userId) {
        SysUser u = DATA.get(SysUser.class, userId);
        String player = u == null ? "" : u.getUserName();
        UserAuthRoleVO vo = new UserAuthRoleVO();
        vo.setRoles(roleOptions());
        vo.setRoleIds(userRoleIds(player));
        // 可分配权限节点全集（菜单 perms + 数据权限 + 模块权限）
        vo.setAllPerms(allPermNodes());
        // 菜单权限树（权限分配弹窗 el-tree 数据）
        vo.setMenuTree(buildMenuTree());
        // 用户直接权限节点（仅 USER 类型，不含组继承）
        vo.setPerms(userDirectPermNodes(player));
        vo.setUser(u);
        return AjaxResult.success(vo);
    }

    /** 菜单权限树：从 erp_menu 构建（menuId/parentId/menuName/perms/menuType/children）。 */
    private List<MenuTreeVO> buildMenuTree() {
        List<SysMenu> all = DATA.select(SysMenu.class);
        Map<String, MenuTreeVO> byId = new LinkedHashMap<>();
        for (SysMenu m : all) {
            MenuTreeVO v = new MenuTreeVO();
            v.setMenuId(m.getMenuId());
            v.setParentId(m.getParentId());
            v.setMenuName(m.getMenuName());
            v.setPerms(m.getPerms());
            v.setMenuType(m.getMenuType());
            byId.put(m.getMenuId(), v);
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO v : byId.values()) {
            MenuTreeVO parent = byId.get(v.getParentId());
            if (parent != null && !parent.equals(v)) {
                parent.getChildren().add(v);
            } else {
                roots.add(v);
            }
        }
        return roots;
    }

    @Override
    public AjaxResult authRoleSave(AuthRoleSaveVO vo) {
        String userId = vo.getUserId() == null ? "" : vo.getUserId();
        SysUser u = userId.isEmpty() ? null : DATA.get(SysUser.class, userId);
        if (u == null) {
            return AjaxResult.error("用户不存在");
        }
        String player = u.getUserName();
        LocalPermissionStore store = auth.getPermissionStore();
        if (store == null) {
            return AjaxResult.error("SOYS 权限存储不可用");
        }
        // 目标组集合
        Set<String> target = new LinkedHashSet<>();
        if (vo.getRoleIds() != null) {
            for (String g : vo.getRoleIds()) {
                if (g != null && !g.trim().isEmpty()) {
                    target.add(g.trim().toLowerCase());
                }
            }
        }
        // 同步：移除已去掉的组、补上新加的组
        for (String g : store.listUserGroups(player)) {
            if (!target.contains(g)) {
                store.removeUserGroup(player, g);
            }
        }
        for (String g : target) {
            store.addUserGroup(player, g);
        }
        operLog.record("用户管理", "分配角色", player, "角色同步成功");
        return AjaxResult.success("角色同步成功");
    }

    @Override
    public AjaxResult listPerms(String userName) {
        UserPermsVO vo = new UserPermsVO();
        vo.setAll(allPermNodes());
        vo.setDirect(userDirectPermNodes(userName));
        vo.setOwned(effectivePermNodes(userName));
        return AjaxResult.success(vo);
    }

    @Override
    public AjaxResult savePerms(String userName, SavePermsVO vo) {
        if (userName == null || userName.isEmpty()) {
            return AjaxResult.error("缺少用户名");
        }
        LocalPermissionStore store = auth.getPermissionStore();
        if (store == null) {
            return AjaxResult.error("SOYS 权限存储不可用");
        }
        Set<String> target = new LinkedHashSet<>();
        if (vo.getPermissions() != null) {
            for (String p : vo.getPermissions()) {
                if (p != null && !p.trim().isEmpty()) {
                    target.add(p.trim());
                }
            }
        }
        // 同步直接权限节点（仅 USER 类型）
        for (String p : userDirectPermNodes(userName)) {
            if (!target.contains(p)) {
                store.removeUserPermission(userName, p);
            }
        }
        for (String p : target) {
            store.addUserPermission(userName, p);
        }
        operLog.record("用户管理", "分配权限", userName, "权限同步成功");
        return AjaxResult.success("权限同步成功");
    }

    @Override
    public AjaxResult profile(CredentialPresentation credential) {
        String player = auth.currentPlayer(credential);
        if (player == null) {
            return AjaxResult.error("未登录");
        }
        ProfileVO vo = new ProfileVO();
        Map<String, Object> info = auth.getInfo(credential);
        Object user = info.get("data") instanceof Map ? ((Map<?, ?>) info.get("data")).get("user") : null;
        vo.setUser(user);
        vo.setRoleGroup(auth.isOp(player) ? "超级管理员" : "普通玩家");
        vo.setPostGroup("ERP 后台");
        return AjaxResult.success(vo);
    }

    @Override
    public AjaxResult updatePwd() {
        return AjaxResult.error("密码由游戏内 AuthMe 管理，请在游戏中使用 /changepassword 修改");
    }

    @Override
    public AjaxResult avatar() {
        return AjaxResult.error("头像上传暂未启用（SOYS 上传接口未接入）");
    }

    // ===== SOYS 权限对接 =====

    /** 角色下拉：SOYS 权限组列表 → RoleOptionVO。 */
    private List<RoleOptionVO> roleOptions() {
        List<RoleOptionVO> out = new ArrayList<>();
        LocalPermissionStore store = auth.getPermissionStore();
        if (store == null) {
            return out;
        }
        for (SoysPermGroup g : store.listGroups()) {
            RoleOptionVO vo = new RoleOptionVO();
            vo.setRoleId(g.getId());
            vo.setRoleName(g.getDisplay() == null || g.getDisplay().isEmpty() ? g.getId() : g.getDisplay());
            vo.setStatus("0");
            out.add(vo);
        }
        return out;
    }

    /** 用户已分配组 id 列表。 */
    private List<String> userRoleIds(String player) {
        LocalPermissionStore store = auth.getPermissionStore();
        if (store == null || player == null || player.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(store.listUserGroups(player));
    }

    /** 用户直接权限节点（USER 类型，含否定前缀还原；SOYS 归一化点→菜单 perms 冒号形式，保证与 allPerms 回显一致）。 */
    private List<String> userDirectPermNodes(String player) {
        LocalPermissionStore store = auth.getPermissionStore();
        List<String> out = new ArrayList<>();
        if (store == null || player == null || player.isEmpty()) {
            return out;
        }
        for (SoysPermPermission p : store.listUserPermissions(player)) {
            out.add((p.isNegative() ? "-" : "") + p.getPermission().replace(".", ":"));
        }
        return out;
    }

    /** 用户有效权限节点（组继承 + 直接；同样还原为菜单 perms 冒号形式）。 */
    private List<String> effectivePermNodes(String player) {
        LocalPermissionStore store = auth.getPermissionStore();
        List<String> out = new ArrayList<>();
        if (store == null || player == null || player.isEmpty()) {
            return out;
        }
        for (SoysPermPermission p : store.listEffectivePermissions(player)) {
            out.add(p.getPermission().replace(".", ":"));
        }
        return out;
    }

    /** 可分配权限节点全集：仅菜单表 perms（菜单权限标识），分配操作直接写 SOYS 本地权限存储。 */
    private List<String> allPermNodes() {
        Set<String> nodes = new LinkedHashSet<>();
        for (SysMenu m : DATA.select(SysMenu.class)) {
            String perm = m.getPerms();
            if (perm != null && !perm.isEmpty()) {
                nodes.add(perm);
            }
        }
        return new ArrayList<>(nodes);
    }

    // ===== 内部 =====

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
