package com.github.cocosoys.mc.mcerp.service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码实现（无登录插件提供者时的第二校验）：
 * 4 位随机字符 + 干扰线，base64 PNG 返回前端；uuid 内存存储，5 分钟过期，校验后即焚。
 */
public class CaptchaServiceImpl implements CaptchaService {

    /** 去掉易混淆字符 0O1I */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int EXPIRE_MS = 5 * 60 * 1000;

    private final Map<String, String> codes = new ConcurrentHashMap<>();
    private final Map<String, Long> expires = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Override
    public Map<String, String> newCaptcha() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        String code = sb.toString();
        String uuid = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        codes.put(uuid, code);
        expires.put(uuid, now + EXPIRE_MS);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("uuid", uuid);
        out.put("img", generateImage(code));
        return out;
    }

    @Override
    public boolean verify(String uuid, String input) {
        if (uuid == null || input == null) {
            return false;
        }
        Long expireAt = expires.remove(uuid);
        String saved = codes.remove(uuid);
        if (saved == null || expireAt == null) {
            return false;
        }
        if (System.currentTimeMillis() > expireAt) {
            return false;
        }
        return saved.equalsIgnoreCase(input.trim());
    }

    private String generateImage(String code) {
        int w = 120;
        int h = 40;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(180, 190, 205));
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 6; i++) {
                g.drawLine(random.nextInt(w), random.nextInt(h), random.nextInt(w), random.nextInt(h));
            }
            g.setFont(new Font("Arial", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(40 + random.nextInt(80), 60 + random.nextInt(80), 120 + random.nextInt(90)));
                int x = 12 + i * 26;
                int y = 28 + random.nextInt(6);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
            }
            for (int i = 0; i < 30; i++) {
                g.setColor(new Color(160 + random.nextInt(60), 160 + random.nextInt(60), 160 + random.nextInt(60)));
                g.fillRect(random.nextInt(w), random.nextInt(h), 1, 1);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }
}
