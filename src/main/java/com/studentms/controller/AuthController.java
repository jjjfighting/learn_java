package com.studentms.controller;

import com.studentms.annotation.OperationLog;
import com.studentms.common.Result;
import com.studentms.common.UserContext;
import com.studentms.common.UserInfo;
import com.studentms.dto.LoginDTO;
import com.studentms.service.AuthService;
import com.studentms.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 * <pre>
 * POST /auth/login   登录换 token（在 WebMvcConfig 白名单里，无需携带 token）
 * GET  /auth/me      查看当前登录用户（演示 UserContext 的用法）
 * </pre>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @OperationLog(module = "auth", action = "LOGIN")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    /** 当前用户信息直接取自线程上下文——拦截器已经验过 token 了，这里不碰数据库 */
    @GetMapping("/me")
    public Result<UserInfo> me() {
        return Result.success(UserContext.get());
    }
}
