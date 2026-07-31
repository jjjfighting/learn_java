package com.studentms.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentms.annotation.RequireRole;
import com.studentms.common.Result;
import com.studentms.common.ResultCode;
import com.studentms.common.UserContext;
import com.studentms.common.UserInfo;
import com.studentms.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;

/**
 * JWT 认证 + 角色鉴权拦截器：所有受保护接口的统一大门
 * <p>
 * 执行时机在 Controller 之前：请求进来 -> 验 token -> 查角色 -> 放行或拦截。
 * 三步走：
 * 1. 没有合法 token            -> 401（未登录或登录过期）
 * 2. token 合法但角色不满足     -> 403（无权限）
 * 3. 全部通过                  -> 用户信息塞进 UserContext，放行
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /** SpringBoot 自动配置的 JSON 序列化器，复用它保证响应格式与 @RestController 完全一致 */
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 非 Controller 方法的请求（如静态资源）直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 第一步：认证——有没有合法身份
        UserInfo user = resolveUser(request);
        if (user == null) {
            writeResult(response, Result.error(ResultCode.UNAUTHORIZED));
            return false;
        }

        // 第二步：鉴权——身份够不够格。没标注 @RequireRole 的方法 = 登录即可访问
        RequireRole required = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (required != null && !Arrays.asList(required.value()).contains(user.getRole())) {
            writeResult(response, Result.error(ResultCode.FORBIDDEN,
                    "权限不足：需要角色 " + String.join("/", required.value()) + "，当前角色 " + user.getRole()));
            return false;
        }

        // 第三步：放行前存入线程上下文，供本次请求的业务代码取用
        UserContext.set(user);
        return true;
    }

    /**
     * 请求完成后必定执行（哪怕 Controller 抛了异常）：清理 ThreadLocal。
     * 不清理 = 线程复用时串用户，见 UserContext 类上的警告。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.remove();
    }

    /** 从 Authorization: Bearer xxx 头取 token 并解析；任何失败（无头 / 篡改 / 过期）都按未登录处理 */
    private UserInfo resolveUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtUtil.parseToken(header.substring(7));
        } catch (Exception e) {
            // ExpiredJwtException / 签名不符 / 格式损坏……对外无需区分，统一"未登录"
            return null;
        }
    }

    /**
     * 拦截器里不能用全局异常处理器那套返回值，要自己把 Result 写成 JSON 响应。
     * HTTP 状态码保持 200——与全项目"成败只看 body.code"的约定一致。
     */
    private void writeResult(HttpServletResponse response, Result<?> result) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
