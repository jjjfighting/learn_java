package com.studentms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学生管理系统启动类
 * <p>
 * @SpringBootApplication 是一个组合注解，包含：
 * 1. @Configuration       —— 标记当前类是配置类，可以写 @Bean 方法
 * 2. @EnableAutoConfiguration —— 开启自动配置（SpringBoot 的核心魔法）
 * 3. @ComponentScan       —— 默认扫描本类所在包（com.studentms）及其子包下的所有组件
 * 所以项目里所有类都要放在 com.studentms 包或它的子包下，否则扫描不到
 */
@SpringBootApplication
public class StudentMsApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentMsApplication.class, args);
    }
}
