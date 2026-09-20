package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 用户状态变更请求体（changeStatus）：userId + status。
 */
@Data
public class ChangeStatusVO {

    /** 用户 ID（erp_user.user_id） */
    private String userId;

    /** 目标状态：'0' 正常 / '1' 停用 */
    private String status;
}
