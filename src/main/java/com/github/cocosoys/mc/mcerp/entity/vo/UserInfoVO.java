package com.github.cocosoys.mc.mcerp.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.cocosoys.mc.soyshttpovermc.orm.convertor.BeanCodec;
import lombok.Data;

import java.util.Date;

/**
 * 用户信息 VO（getInfo / 个人中心 profile 接口返回的 user 字段）。
 */
@Data
public class UserInfoVO {

    /** 用户 ID（当前为登录玩家名语义） */
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

    /** 创建时间 */
    @JsonFormat(pattern = BeanCodec.DATE_TIME_PATTERN)
    private Date createTime;
}
