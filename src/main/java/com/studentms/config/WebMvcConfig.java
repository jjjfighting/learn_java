package com.studentms.config;

import com.studentms.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SpringMVC 配置：注册 JWT 拦截器
 * <p>
 * 默认拦截一切（/**），只放行四类：
 * - /auth/login    登录接口本身不能要求先登录（鸡生蛋问题）
 * - /files/view/** 文件读取公开——头像要能在 img 标签里直接加载，而 img 没法带 Authorization 头
 *                  （上传接口 /files/upload 不在此列，仍需登录）
 * - /test/**       第三步留下的公共层验证接口
 * - /error         SpringBoot 默认错误页转发路径，拦了会死循环
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login", "/files/view/**", "/test/**", "/error");
    }
}
