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
     * 返回顶层 {user, roles, posts}，供岗位/角色下拉与创建人展示。
     */
    AjaxResult formInit(CredentialPresentation credential);

    AjaxResult detail(String userId);

    AjaxResult add(ErpUser user);

    AjaxResult update(String userId, ErpUser user);

    AjaxResult remove(String userIds, CredentialPresentation credential);

    AjaxResult changeStatus(ChangeStatusVO vo);

    AjaxResult resetPwd();

    AjaxResult deptTree();

    AjaxResult authRole(String userId);

    AjaxResult authRoleSave(AuthRoleSaveVO vo);

    AjaxResult listPerms(String userName);

    AjaxResult savePerms(String userName, SavePermsVO vo);

    AjaxResult profile(CredentialPresentation credential);

    AjaxResult updatePwd();

    AjaxResult avatar();
}
