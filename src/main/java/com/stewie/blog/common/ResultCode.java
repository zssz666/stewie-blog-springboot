package com.stewie.blog.common;

import lombok.Getter;

/**
 * 统一响应码枚举
 */
@Getter
public enum ResultCode {

    // 通用成功
    SUCCESS(200, "success"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方式不支持"),

    // 服务端错误 5xx
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务错误 - 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    ACCOUNT_DISABLED(1004, "账号已禁用"),

    // 业务错误 - 文章模块 2xxx
    POST_NOT_FOUND(2001, "文章不存在"),
    POST_SLUG_EXISTS(2002, "文章路径已存在"),
    POST_STATUS_ERROR(2003, "文章状态非法"),

    // 业务错误 - 评论模块 3xxx
    COMMENT_NOT_FOUND(3001, "评论不存在"),
    COMMENT_REVIEWED(3002, "评论已审核，不可重复操作"),
    COMMENT_CONTENT_TOO_LONG(3003, "评论内容过长"),

    // 业务错误 - 互动模块 4xxx
    ALREADY_LIKED(4001, "已点赞过该文章"),

    // 业务错误 - 认证模块 5xxx
    TOKEN_INVALID(5001, "Token 无效"),
    TOKEN_EXPIRED(5002, "Token 已过期"),
    REFRESH_TOKEN_INVALID(5003, "刷新令牌无效");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
