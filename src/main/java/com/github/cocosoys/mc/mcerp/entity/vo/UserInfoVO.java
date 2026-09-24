package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 用户信息 VO（getInfo 接口返回的 user 字段）。
 */
@Data
public class UserInfoVO {

    /** 用户 ID */
    private String userId;

    /** 登录账号 */
    private String userName;

    /** 昵称 */
    private String nickName;

    /** 头像地址 */
    private String avatar;

    /** 性别（0男/1女/2未知） */
    private String sex;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phonenumber;

    /** 状态（0正常/1停用） */
    private String status;
}
