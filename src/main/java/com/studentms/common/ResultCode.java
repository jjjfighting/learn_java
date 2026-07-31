package com.studentms.common;

import lombok.Getter;

/**
 * 返回码枚举：集中管理所有接口返回码，避免魔法数字散落在各处代码里
 * <p>
 * 编码约定（对齐 HTTP 语义 + 业务扩展）：
 * <pre>
 * 200        成功
 * 400 ~ 499  客户端错误（参数错误 / 未登录 / 无权限 / 资源不存在）
 * 500        服务器内部错误
 * 4 位业务码  第 1 位是模块号：1=用户模块 2=学生模块 3=班级/课程模块 4=成绩模块
 * </pre>
 */
@Getter
public enum ResultCode {

    // ========================= 通用码 =========================
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限执行此操作"),
    NOT_FOUND(404, "请求的资源不存在"),
    INTERNAL_SERVER_ERROR(500, "系统繁忙，请稍后再试"),

    // ========================= 用户模块 1xxx =========================
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_ALREADY_EXISTS(1002, "用户名已存在"),
    WRONG_CREDENTIALS(1003, "用户名或密码错误"),
    TEACHER_ROLE_REQUIRED(1004, "目标用户不是教师，不能指定为授课教师"),

    // ========================= 学生模块 2xxx =========================
    STUDENT_NOT_FOUND(2001, "学生不存在"),
    STUDENT_NO_ALREADY_EXISTS(2002, "学号已存在"),

    // ========================= 班级模块 3xxx =========================
    CLAZZ_NOT_FOUND(3001, "班级不存在"),
    CLAZZ_HAS_STUDENTS(3002, "该班级下还有学生，禁止删除"),
    COURSE_NOT_FOUND(3003, "课程不存在"),
    COURSE_HAS_SCORES(3004, "该课程已有成绩记录，禁止删除"),

    // ========================= 成绩模块 4xxx =========================
    SCORE_ALREADY_EXISTS(4001, "该学生在此课程已有成绩记录"),
    SCORE_NOT_FOUND(4002, "成绩记录不存在");

    /** 业务码 */
    private final Integer code;

    /** 默认提示消息 */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
