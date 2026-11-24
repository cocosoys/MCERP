package com.github.cocosoys.mc.mcerp.entity.vo;

import lombok.Data;

/**
 * 头像上传请求 VO：前端裁剪后以 base64 data URL 提交（替代 multipart/form-data）。
 *
 * <p>框架不内置 multipart 解析，故前端用 vue-cropper 的 getCropData 生成 data URL，
 * 经 JSON 提交本 VO，后端解码后落盘并登记静态托管，避免在控制器/服务里手工解析 boundary。</p>
 */
@Data
public class AvatarUploadVO {

    /** 裁剪后头像的 base64 data URL（如 data:image/png;base64,xxxx） */
    private String img;
}
