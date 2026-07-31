package com.studentms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：打在 Controller 方法上，如
 * <pre>
 * &#64;OperationLog(module = "student", action = "CREATE")
 * </pre>
 * 拦截逻辑全部在 OperationLogAspect 里——业务方法不用写一行日志代码，
 * 这就是 AOP"非侵入式横切关注点"的价值。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 模块名：student / clazz / course / score / auth / file */
    String module();

    /** 动作名：CREATE / UPDATE / DELETE / LOGIN / UPLOAD ... */
    String action();
}
