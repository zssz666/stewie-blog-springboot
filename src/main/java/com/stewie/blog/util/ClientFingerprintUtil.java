package com.stewie.blog.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 客户端指纹 / IP 工具
 * <p>点赞防刷：用「客户端 IP + User-Agent」生成稳定指纹；IP 优先取 X-Forwarded-For / X-Real-IP（反代场景）。</p>
 */
public final class ClientFingerprintUtil {

    private ClientFingerprintUtil() {
    }

    /**
     * 生成指纹：MD5(客户端IP + "|" + User-Agent)
     */
    public static String getFingerprint(HttpServletRequest request) {
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");
        if (ua == null) {
            ua = "";
        }
        return DigestUtils.md5DigestAsHex((ip + "|" + ua).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获取真实客户端 IP（兼容 Nginx / 云负载均衡）
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return (idx > 0 ? ip.substring(0, idx) : ip).trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
