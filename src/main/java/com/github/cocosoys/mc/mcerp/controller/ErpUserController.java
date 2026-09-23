package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.ErpUser;
import com.github.cocosoys.mc.mcerp.entity.vo.AuthRoleSaveVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ChangeStatusVO;
import com.github.cocosoys.mc.mcerp.entity.vo.SavePermsVO;
import com.github.cocosoys.mc.mcerp.service.ErpUserService;
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
 * 用户管理（若依契约）：路由 /api/plugins/MCERP/system/user/*。
 * 仅做参数绑定与权限声明，业务逻辑（CRUD、状态、角色/权限同步）全部委托 {@link ErpUserService}。
 */
@RequestMapping("/system/user")
/**
 * 用户管理控制器：用户 CRUD/状态/权限/角色分配。
 */
public class ErpUserController {

    private final ErpUserService userService;

    public ErpUserController(ErpUserService userService) {
        this.userService = userService;
    }

    @ApiName("用户列表")
    @ApiPermission("mcerp:system:user:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "userName", required = false) String userName,
                              @RequestParam(name = "status", required = false) String status,
                              @RequestParam(name = "phonenumber", required = false) String phonenumber) {
        return userService.list(pageNum, pageSize, userName, status, phonenumber);
    }

    @ApiName("新增用户表单初始化")
    @ApiPermission("mcerp:system:user:query")
    @GetMapping("/")
    public AjaxResult formInit(CredentialPresentation credential) {
        return userService.formInit(credential);
    }

    @ApiName("用户详情")
    @ApiPermission("mcerp:system:user:query")
    @GetMapping("/{userId}")
    public AjaxResult detail(@PathVariable(name = "userId") String userId) {
        return userService.detail(userId);
    }

    @ApiName("新增用户")
    @ApiPermission("mcerp:system:user:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody ErpUser user) {
        return userService.add(user);
    }

    @ApiName("编辑用户")
    @ApiPermission("mcerp:system:user:edit")
    @PutMapping("/{userId}")
    public AjaxResult update(@PathVariable(name = "userId") String userId, @RequestBody ErpUser user) {
        return userService.update(userId, user);
    }

    @ApiName("删除用户")
    @ApiPermission("mcerp:system:user:remove")
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable(name = "userIds") String userIds,
                             CredentialPresentation credential) {
        return userService.remove(userIds, credential);
    }

    @ApiName("用户状态修改")
    @ApiPermission("mcerp:system:user:changeStatus")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody ChangeStatusVO vo) {
        return userService.changeStatus(vo);
    }

    @ApiName("重置密码")
    @ApiPermission("mcerp:system:user:resetPwd")
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd() {
        return userService.resetPwd();
    }

    @ApiName("部门树")
    @ApiPermission("mcerp:system:user:list")
    @GetMapping("/deptTree")
    public AjaxResult deptTree() {
        return userService.deptTree();
    }

    @ApiName("用户角色")
    @ApiPermission("mcerp:system:user:query")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable(name = "userId") String userId) {
        return userService.authRole(userId);
    }

    @ApiName("用户角色保存")
    @ApiPermission("mcerp:system:user:edit")
    @PutMapping("/authRole")
    public AjaxResult authRoleSave(@RequestBody AuthRoleSaveVO vo) {
        return userService.authRoleSave(vo);
    }

    @ApiName("用户权限列表")
    @ApiPermission("mcerp:system:user:edit")
    @GetMapping("/permissions/{userName}")
    public AjaxResult listPerms(@PathVariable(name = "userName") String userName) {
        return userService.listPerms(userName);
    }

    @ApiName("用户权限保存")
    @ApiPermission("mcerp:system:user:edit")
    @PutMapping("/permissions/{userName}")
    public AjaxResult savePerms(@PathVariable(name = "userName") String userName, @RequestBody SavePermsVO vo) {
        return userService.savePerms(userName, vo);
    }

    @ApiName("个人中心")
    @ApiPublic // 网关 auth 已保护 /api/*（无凭证 401），此处仅解除注册表默认拒绝 → 登录即可
    @GetMapping("/profile")
    public AjaxResult profile(CredentialPresentation credential) {
        return userService.profile(credential);
    }

    @ApiName("修改密码")
    @ApiPublic // 网关 401 兜底，登录即可（密码由 AuthMe 管理，返回引导提示）
    @PutMapping("/profile/updatePwd")
    public AjaxResult updatePwd() {
        return userService.updatePwd();
    }

    @ApiName("头像上传")
    @ApiPublic // 网关 401 兜底，登录即可（暂未启用，返回引导提示）
    @PostMapping("/profile/avatar")
    public AjaxResult avatar() {
        return userService.avatar();
    }
}
