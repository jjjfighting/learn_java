package com.studentms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色访问控制注解：标注在 Controller 方法上，如 @RequireRole("ADMIN")
 * <p>
 * 检查逻辑在 JwtAuthInterceptor 里：登录态校验通过后，再比对方法要求的角色
 * 与令牌里的角色，不匹配返回 403。
 * <p>
 * 不写这个注解的接口 = 任何已登录用户都能访问（读操作基本都是这类）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)   // 必须保留到运行时，拦截器才能反射读到
public @interface RequireRole {

    /** 允许访问的角色列表，满足其一即可 */
    String[] value();
}
