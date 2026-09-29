package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.entity.ErpUser;
import com.github.cocosoys.mc.mcerp.entity.vo.AuthRoleSaveVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ChangeStatusVO;
import com.github.cocosoys.mc.mcerp.entity.vo.SavePermsVO;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 用户管理服务（抽象契约）：用户 CRUD、状态、角色/权限对接 SOYS 权限存储。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.ErpUserServiceImpl}；
 * 角色（权限组）与权限节点同步等一致性编排由 impl 负责。
 */
public interface ErpUserService {

    TableDataInfo list(Integer pageNum, Integer pageSize, String userName, String status, String phonenumber);

    /**
     * 新增用户表单初始化（RuoYi 前端 handleAdd 调 GET /system/user/）：
     * 返回顶层 {user, roles}，供角色下拉与创建人展示。
     */
    AjaxResult formInit(CredentialPresentation credential);

    AjaxResult detail(String userId);

    AjaxResult add(ErpUser user, CredentialPresentation credential);

    AjaxResult update(String userId, ErpUser user, CredentialPresentation credential);

    AjaxResult remove(String userIds, CredentialPresentation credential);

    AjaxResult changeStatus(ChangeStatusVO vo, CredentialPresentation credential);

    AjaxResult resetPwd();


    AjaxResult authRole(String userId);

    AjaxResult authRoleSave(AuthRoleSaveVO vo, CredentialPresentation credential);

    AjaxResult listPerms(String userName);

    AjaxResult savePerms(String userName, SavePermsVO vo, CredentialPresentation credential);

    AjaxResult profile(CredentialPresentation credential);

    /**
     * 更新个人资料（RuoYi 前端个人中心保存调 PUT /system/user/profile）：
     * 按当前登录玩家（凭证主体）定位 erp_user，未登记时自动补登记；
     * 仅更新可编辑字段（昵称/邮箱/手机/性别/备注）。
     */
    AjaxResult updateProfile(ErpUser user, CredentialPresentation credential);

    AjaxResult updatePwd();

    AjaxResult avatar();
}
