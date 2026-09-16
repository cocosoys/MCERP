package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 用户状态变更请求体（changeStatus）：userId + status。
 */
@Data
public class ChangeStatusVO {

    private String userId;
    private String status;
}
