package com.studentms.common;

/**
 * 当前请求的登录用户上下文（ThreadLocal 版）
 * <p>
 * 拦截器验完 token 把 UserInfo 放进来，本次请求内任何一层（Controller / Service）
 * 都能 UserContext.get() 拿到当前用户，不用一层层往下传参。
 * <p>
 * ⚠️ 必须配对使用 set / remove：Tomcat 用线程池复用线程，请求结束不清理的话，
 * 下一个请求复用同一线程就会"读到上一个用户"——经典的越权事故来源。
 * 清理由拦截器的 afterCompletion 统一负责。
 */
public final class UserContext {

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserInfo user) {
        HOLDER.set(user);
    }

    public static UserInfo get() {
        return HOLDER.get();
    }

    public static void remove() {
        HOLDER.remove();
    }
}
