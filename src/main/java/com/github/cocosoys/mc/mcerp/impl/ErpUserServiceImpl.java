package com.github.cocosoys.mc.mcerp.impl;

import static com.github.cocosoys.mc.mcerp.i18n.McerpI18n.t;

import com.github.cocosoys.mc.mcerp.entity.ErpMenu;
import com.github.cocosoys.mc.mcerp.entity.ErpUser;
import com.github.cocosoys.mc.mcerp.entity.vo.MenuTreeVO;
import com.github.cocosoys.mc.mcerp.enums.BusinessType;
import com.github.cocosoys.mc.mcerp.entity.vo.AvatarUploadVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ProfileVO;
import com.github.cocosoys.mc.mcerp.entity.vo.RoleOptionVO;
import com.github.cocosoys.mc.mcerp.entity.vo.UserAuthRoleVO;
import com.github.cocosoys.mc.mcerp.entity.vo.UserDetailVO;
import com.github.cocosoys.mc.mcerp.entity.vo.AuthRoleSaveVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ChangeStatusVO;
import com.github.cocosoys.mc.mcerp.entity.vo.SavePermsVO;
import com.github.cocosoys.mc.mcerp.entity.vo.UserPermsVO;
import com.github.cocosoys.mc.mcerp.entity.vo.UserInfoVO;
import com.github.cocosoys.mc.mcerp.ErpRegistry;
import com.github.cocosoys.mc.mcerp.MCERP;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpMenuVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpModuleVO;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.ErpUserService;
import com.github.cocosoys.mc.mcerp.util.Ids;
import com.github.cocosoys.mc.mcerp.util.NameSafe;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysHttpOverMcApi;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.permission.local.LocalPermissionStore;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.SoysPermGroup;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.SoysPermPermission;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户管理实现（从 ErpUserController 迁入）：CRUD、状态、角色/权限同步。
 * 角色（权限组）与权限节点（USER 类型）的增删同步在此编排，保证多步一致。
 */
public class ErpUserServiceImpl implements ErpUserService {

    /** 头像磁盘目录名（数据文件夹下）与对应 web 基础路径（registerDirectory 会自动补 /web/plugins/&lt;插件名&gt; 前缀）。 */
    public static final String AVATAR_DIR_NAME = "avatar";
    public static final String AVATAR_BASE_PATH = "/avatar";

    /** 头像目录是否已批量登记（进程内幂等，避免重复扫描/重复登记）。 */
    private static volatile boolean avatarDirRegistered = false;

    /**
     * 启动时缓存的 SOYS API 门面：由 {@link McErpHostExpansion#registerPages} 经
     * {@link #registerAvatarDirectory(SoysHttpOverMcApi)} 注入；reload 重建门面时幂等覆盖。
     * 框架的 {@code getApi()} 为实例方法，静态头像工具经此缓存引用访问门面。
     */
    private static volatile SoysHttpOverMcApi avatarApi;

    private final AuthService auth;
    private final OperLogService operLog;
    private final ErpRegistry registry;

    public ErpUserServiceImpl(AuthService auth, OperLogService operLog, ErpRegistry registry) {
        this.auth = auth;
        this.operLog = operLog;
        this.registry = registry;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String userName, String status, String phonenumber) {
        List<ErpUser> all = DATA.select(ErpUser.class);
        List<ErpUser> filtered = new ArrayList<>();
        for (ErpUser u : all) {
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
        filtered.sort(Comparator.comparing(ErpUser::getCreateTime, Comparator.nullsLast(Date::compareTo)).reversed());
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult formInit(CredentialPresentation credential) {
        AjaxResult ok = AjaxResult.success();
        // 当前登录者（新增表单顶部展示 / 创建人）
        String player = auth.currentPlayer(credential);
        UserInfoVO user = new UserInfoVO();
        user.setUserId(player == null ? "" : player);
        user.setUserName(player == null ? "" : player);
        user.setNickName(player == null ? "" : player);
        ok.put("user", user);
        // 角色下拉（SOYS 权限组）
        ok.put("roles", roleOptions());
        return ok;
    }

    @Override
    public AjaxResult detail(String userId) {
        Long id = Ids.parse(userId);
        ErpUser u = id == null ? null : DATA.get(ErpUser.class, id);
        if (u == null) {
            return AjaxResult.error(t("mcerp.user.not-found", "用户不存在"));
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
        return AjaxResult.success(vo);
    }

    @Override
    public AjaxResult add(ErpUser user, CredentialPresentation credential) {
        String userName = user.getUserName() == null ? "" : user.getUserName().trim();
        if (userName.isEmpty()) {
            return AjaxResult.error(t("mcerp.user.username-empty", "用户名不能为空"));
        }
        if (auth.findUser(userName) != null) {
            return AjaxResult.error(t("mcerp.user.username-exists", "用户名已存在"));
        }
        ErpUser u = new ErpUser();
        u.setUserName(userName);
        u.setNickName(user.getNickName() == null || user.getNickName().isEmpty() ? userName : user.getNickName());
        u.setEmail(user.getEmail() == null ? "" : user.getEmail());
        u.setPhonenumber(user.getPhonenumber() == null ? "" : user.getPhonenumber());
        u.setSex(user.getSex() == null || user.getSex().isEmpty() ? "0" : user.getSex());
        u.setStatus(user.getStatus() == null || user.getStatus().isEmpty() ? "0" : user.getStatus());
        u.setRemark(user.getRemark() == null ? "" : user.getRemark());
        u.setCreateTime(AuthService.now());
        DATA.insert(u);
        operLog.record(credential, t("mcerp.operlog.module.user", "用户管理"), BusinessType.INSERT, t("mcerp.operlog.action.add-user", "新增用户"), userName, t("mcerp.common.add-success", "新增成功"));
        return AjaxResult.success(t("mcerp.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(String userId, ErpUser user, CredentialPresentation credential) {
        Long id = Ids.parse(userId);
        ErpUser u = id == null ? null : DATA.get(ErpUser.class, id);
        if (u == null) {
            return AjaxResult.error(t("mcerp.user.not-found", "用户不存在"));
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
        operLog.record(credential, t("mcerp.operlog.module.user", "用户管理"), BusinessType.UPDATE, t("mcerp.operlog.action.edit-user", "修改用户"), u.getUserName(), t("mcerp.common.edit-success", "修改成功"));
        return AjaxResult.success(t("mcerp.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String userIds, CredentialPresentation credential) {
        if (userIds == null || userIds.isEmpty()) {
            return AjaxResult.error(t("mcerp.user.missing-userId", "缺少 userId"));
        }
        String current = auth.currentPlayer(credential);
        for (String id : userIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            Long uid = Ids.parse(id);
            ErpUser u = uid == null ? null : DATA.get(ErpUser.class, uid);
            if (u != null && current != null && u.getUserName().equalsIgnoreCase(current)) {
                return AjaxResult.error(t("mcerp.user.cannot-delete-self", "不能删除当前登录账号"));
            }
            DATA.deleteById(ErpUser.class, uid);
            operLog.record(credential, t("mcerp.operlog.module.user", "用户管理"), BusinessType.DELETE, t("mcerp.operlog.action.delete-user", "删除用户"), u == null ? id.trim() : u.getUserName(), t("mcerp.common.delete-success", "删除成功"));
        }
        return AjaxResult.success(t("mcerp.common.delete-success", "删除成功"));
    }

    @Override
    public AjaxResult changeStatus(ChangeStatusVO vo, CredentialPresentation credential) {
        ErpUser u = vo.getUserId() == null ? null : DATA.get(ErpUser.class, vo.getUserId());
        if (u == null) {
            return AjaxResult.error(t("mcerp.user.not-found", "用户不存在"));
        }
        u.setStatus(vo.getStatus() == null || vo.getStatus().isEmpty() ? "0" : vo.getStatus());
        DATA.updateById(u);
        operLog.record(credential, t("mcerp.operlog.module.user", "用户管理"), BusinessType.UPDATE, t("mcerp.operlog.action.change-user-status", "修改用户状态"), u.getUserName(), t("mcerp.common.edit-success", "修改成功"));
        return AjaxResult.success(t("mcerp.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult resetPwd() {
        // 密码完全由 AuthMe 管理（游戏内 /changepassword 或 SOYS 登录桥）
        return AjaxResult.error(t("mcerp.user.password-authme", "密码由游戏内 AuthMe 管理，请在游戏中使用 /changepassword 修改"));
    }


    @Override
    public AjaxResult authRole(String userId) {
        ErpUser u = DATA.get(ErpUser.class, Ids.parse(userId));
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

    /** 菜单权限树：erp_menu 表 + 扩展插件登记菜单合并构建（menuId/parentId/menuName/perms/menuType/children）。 */
    private List<MenuTreeVO> buildMenuTree() {
        List<ErpMenu> all = DATA.select(ErpMenu.class);
        Map<String, MenuTreeVO> byId = new LinkedHashMap<>();
        for (ErpMenu m : all) {
            MenuTreeVO v = new MenuTreeVO();
            v.setMenuId(m.getMenuId() == null ? null : String.valueOf(m.getMenuId()));
            v.setParentId(m.getParentId() == null ? "0" : String.valueOf(m.getParentId()));
            v.setMenuName(m.getMenuName());
            v.setPerms(m.getPerms());
            v.setMenuType(m.getMenuType());
            byId.put(v.getMenuId(), v);
        }
        // 合并扩展插件登记菜单
        if (registry != null) {
            for (ErpModuleVO module : registry.getModules()) {
                String modId = module.getId();
                MenuTreeVO modNode = new MenuTreeVO();
                modNode.setMenuId(modId);
                modNode.setParentId("0");
                modNode.setMenuName(module.getDisplayName());
                modNode.setPerms(module.getPermission());
                modNode.setMenuType("M");
                byId.put(modId, modNode);
                addExtMenuNodes(byId, module.getChildren(), modId, modId);
            }
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

    /** 递归添加扩展插件菜单节点到权限树。childId 用 parentId 链拼路径，避免跨层级 menuId 冲突覆盖。 */
    private void addExtMenuNodes(Map<String, MenuTreeVO> byId, List<ErpMenuVO> menus, String parentId, String moduleId) {
        if (menus == null) {
            return;
        }
        for (ErpMenuVO m : menus) {
            String rawId = m.getMenuId() == null ? m.getMenuName() : m.getMenuId();
            String childId = parentId + "_" + rawId;
            MenuTreeVO v = new MenuTreeVO();
            v.setMenuId(childId);
            v.setParentId(parentId);
            v.setMenuName(m.getMenuName());
            v.setPerms(m.getPerms());
            v.setMenuType(m.getMenuType() == null ? "C" : m.getMenuType());
            byId.put(childId, v);
            addExtMenuNodes(byId, m.getChildren(), childId, moduleId);
        }
    }

    @Override
    public AjaxResult authRoleSave(AuthRoleSaveVO vo, CredentialPresentation credential) {
        ErpUser u = vo.getUserId() == null ? null : DATA.get(ErpUser.class, vo.getUserId());
        if (u == null) {
            return AjaxResult.error(t("mcerp.user.not-found", "用户不存在"));
        }
        String player = u.getUserName();
        LocalPermissionStore store = auth.getPermissionStore();
        if (store == null) {
            return AjaxResult.error(t("mcerp.user.soys-perm-unavailable", "SOYS 权限存储不可用"));
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
        operLog.record(credential, t("mcerp.operlog.module.user", "用户管理"), BusinessType.GRANT, t("mcerp.operlog.action.assign-role", "分配角色"), player, t("mcerp.user.role-sync-success", "角色同步成功"));
        return AjaxResult.success(t("mcerp.user.role-sync-success", "角色同步成功"));
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
    public AjaxResult savePerms(String userName, SavePermsVO vo, CredentialPresentation credential) {
        if (userName == null || userName.isEmpty()) {
            return AjaxResult.error(t("mcerp.user.missing-username", "缺少用户名"));
        }
        LocalPermissionStore store = auth.getPermissionStore();
        if (store == null) {
            return AjaxResult.error(t("mcerp.user.soys-perm-unavailable", "SOYS 权限存储不可用"));
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
        operLog.record(credential, t("mcerp.operlog.module.user", "用户管理"), BusinessType.GRANT, t("mcerp.operlog.action.assign-perm", "分配权限"), userName, t("mcerp.user.perm-sync-success", "权限同步成功"));
        return AjaxResult.success(t("mcerp.user.perm-sync-success", "权限同步成功"));
    }

    @Override
    public AjaxResult profile(CredentialPresentation credential) {
        String player = auth.currentPlayer(credential);
        if (player == null) {
            return AjaxResult.error(t("mcerp.common.not-logged-in", "未登录"));
        }
        ErpUser u = auth.findUser(player);
        ProfileVO vo = new ProfileVO();
        UserInfoVO user = new UserInfoVO();
        user.setUserId(player);
        user.setUserName(player);
        user.setNickName(u != null && u.getNickName() != null && !u.getNickName().isEmpty() ? u.getNickName() : player);
        user.setAvatar(u == null || u.getAvatar() == null ? "" : u.getAvatar());
        user.setSex(u == null ? "0" : u.getSex());
        user.setEmail(u == null ? "" : u.getEmail());
        user.setPhonenumber(u == null ? "" : u.getPhonenumber());
        user.setStatus(u == null ? "0" : u.getStatus());
        user.setCreateTime(u == null ? null : u.getCreateTime());
        vo.setUser(user);
        vo.setRoleGroup(auth.isOp(player) ? t("mcerp.user.rolegroup-op", "超级管理员") : t("mcerp.user.rolegroup-player", "普通玩家"));
        return AjaxResult.success(vo);
    }

    @Override
    public AjaxResult updateProfile(ErpUser user, CredentialPresentation credential) {
        String player = auth.currentPlayer(credential);
        if (player == null) {
            return AjaxResult.error(t("mcerp.common.not-logged-in", "未登录"));
        }
        ErpUser u = auth.findUser(player);
        boolean created = false;
        if (u == null) {
            // 当前玩家未登记过：按个人资料保存补登记（与首次进入自动登记同一字段口径）
            u = new ErpUser();
            u.setUserName(player);
            u.setStatus("0");
            u.setCreateTime(AuthService.now());
            created = true;
        }
        if (user.getNickName() != null && !user.getNickName().isEmpty()) {
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
        if (user.getRemark() != null) {
            u.setRemark(user.getRemark());
        }
        if (created) {
            DATA.insert(u);
        } else {
            DATA.updateById(u);
        }
        operLog.record(credential, t("mcerp.operlog.module.user", "用户管理"), BusinessType.UPDATE, t("mcerp.operlog.action.edit-user", "修改用户"), player, t("mcerp.common.edit-success", "修改成功"));
        return AjaxResult.success(t("mcerp.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult updatePwd() {
        return AjaxResult.error(t("mcerp.user.password-authme", "密码由游戏内 AuthMe 管理，请在游戏中使用 /changepassword 修改"));
    }

    @Override
    public AjaxResult avatar(AvatarUploadVO vo, CredentialPresentation credential) {
        String player = auth.currentPlayer(credential);
        if (player == null) {
            return AjaxResult.error(t("mcerp.common.not-logged-in", "未登录"));
        }
        if (vo == null || vo.getImg() == null || vo.getImg().isEmpty()) {
            return AjaxResult.error(t("mcerp.user.avatar-empty", "头像数据为空"));
        }
        // 解析 data URL：data:image/png;base64,<payload>
        String dataUrl = vo.getImg();
        int comma = dataUrl.indexOf(',');
        if (!dataUrl.startsWith("data:") || comma < 0) {
            return AjaxResult.error(t("mcerp.user.avatar-bad-format", "头像数据格式错误"));
        }
        String header = dataUrl.substring(0, comma);
        String payload = dataUrl.substring(comma + 1);
        int semi = header.indexOf(';');
        String mime = semi > 5 ? header.substring(5, semi) : "image/png";
        if (!mime.startsWith("image/")) {
            return AjaxResult.error(t("mcerp.user.avatar-not-image", "仅支持图片类型"));
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(t("mcerp.user.avatar-decode-failed", "头像解码失败"));
        }
        if (bytes.length == 0) {
            return AjaxResult.error(t("mcerp.user.avatar-decode-failed", "头像解码失败"));
        }
        Plugin owner = MCERP.getInstance();
        if (owner == null) {
            return AjaxResult.error(t("mcerp.user.avatar-host-unavailable", "宿主插件不可用"));
        }
        File avatarDir = new File(owner.getDataFolder(), AVATAR_DIR_NAME);
        if (!avatarDir.isDirectory() && !avatarDir.mkdirs()) {
            return AjaxResult.error(t("mcerp.user.avatar-dir-failed", "头像目录创建失败"));
        }
        // 目录登记（重启后已存在的头像可继续访问）；先于写新文件，避免把新文件也纳入目录扫描
        ensureAvatarDirectory();
        String fileName = NameSafe.safe(player) + "-" + System.currentTimeMillis() + extensionOf(mime);
        File target = new File(avatarDir, fileName);
        try {
            Files.write(target.toPath(), bytes);
        } catch (IOException e) {
            return AjaxResult.error(t("mcerp.user.avatar-write-failed", "头像写入失败"));
        }
        // 单文件登记：本次上传立即可访问，无需等待重启
        String imgUrl = avatarWebRoot(owner) + AVATAR_BASE_PATH + "/" + fileName;
        // 登记路径须为相对路径（/avatar/&lt;文件&gt;）：resolvePath 会自动补 /web/plugins/&lt;插件名&gt;，
        // 若传完整 imgUrl 会被二次拼接成双前缀路由，导致 404
        String registerPath = AVATAR_BASE_PATH + "/" + fileName;
        try {
            SoysHttpOverMcApi api = avatarApi;
            if (api != null) {
                api.getWebPage().registerPage(owner, registerPath, bytes, mime, true);
            }
        } catch (Exception e) {
            // 单文件登记失败不阻断保存：目录已登记，重启后启动钩子仍会托管该文件
        }
        // 回填 erp_user.avatar（未登记玩家按补建处理）
        ErpUser u = auth.findUser(player);
        boolean created = false;
        if (u == null) {
            u = new ErpUser();
            u.setUserName(player);
            u.setStatus("0");
            u.setCreateTime(AuthService.now());
            created = true;
        }
        u.setAvatar(imgUrl);
        if (created) {
            DATA.insert(u);
        } else {
            DATA.updateById(u);
        }
        AjaxResult ok = AjaxResult.success(t("mcerp.common.edit-success", "修改成功"));
        ok.put("imgUrl", imgUrl);
        return ok;
    }

    /**
     * 启动时登记头像磁盘目录（由 McErpHostExpansion.registerPages 调用）：
     * 缓存 SOYS API 门面引用；目录不存在（首次安装）时静默跳过；登记失败不阻断页面注册。
     *
     * @param api 宿主扩展的实例 API 门面（{@code SoysExpansion.getApi()}），不允许为 null
     */
    public static void registerAvatarDirectory(SoysHttpOverMcApi api) {
        if (api != null) {
            avatarApi = api;
        }
        ensureAvatarDirectory();
    }

    /**
     * 幂等登记头像目录：仅在数据文件夹/avatar 已存在时批量登记，重复调用由静态标志短路。
     */
    private static void ensureAvatarDirectory() {
        if (avatarDirRegistered) {
            return;
        }
        try {
            Plugin owner = MCERP.getInstance();
            SoysHttpOverMcApi api = avatarApi;
            if (owner != null && api != null) {
                File dir = new File(owner.getDataFolder(), AVATAR_DIR_NAME);
                if (dir.isDirectory()) {
                    api.getWebPage().registerDirectory(owner, AVATAR_BASE_PATH, dir);
                }
            }
        } catch (Exception ignored) {
            // 目录登记失败不阻断：单文件登记兜底，重启后启动钩子会再次尝试
        }
        avatarDirRegistered = true;
    }

    /**
     * 头像 web 根路径：优先经 toolkit 按当前插件名动态推导，失败回退 /web/plugins/&lt;插件名&gt;。
     */
    private static String avatarWebRoot(Plugin owner) {
        SoysHttpOverMcApi api = avatarApi;
        if (api != null) {
            String prefix = api.getToolkit().pageFullPrefix(owner);
            if (prefix != null && !prefix.isEmpty()) {
                return prefix;
            }
        }
        return "/web/plugins/" + owner.getName();
    }

    /** 按 MIME 类型取文件扩展名（默认 png）。 */
    private static String extensionOf(String mime) {
        switch (mime) {
            case "image/jpeg":
            case "image/jpg":
                return ".jpg";
            case "image/gif":
                return ".gif";
            case "image/webp":
                return ".webp";
            case "image/png":
            default:
                return ".png";
        }
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
        for (ErpMenu m : DATA.select(ErpMenu.class)) {
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
