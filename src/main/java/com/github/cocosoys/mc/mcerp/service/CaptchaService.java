package com.github.cocosoys.mc.mcerp.service;

import java.util.Map;

/**
 * 图形验证码服务（抽象契约）：生成与校验。
 * 实现见 {@link CaptchaServiceImpl}（AWT 图形码 + 内存存储）。
 */
public interface CaptchaService {

    /**
     * 生成新验证码。
     *
     * @return {uuid, img}(img 为 base64 PNG 数据，无 data: 前缀，与若依契约一致)
     */
    Map<String, String> newCaptcha();

    /**
     * 校验验证码（校验后即焚，无论成败）。
     */
    boolean verify(String uuid, String input);
}
