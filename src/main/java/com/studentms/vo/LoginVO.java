package com.studentms.vo;

import lombok.Data;

/**
 * 登录成功响应：令牌 + 前端展示需要的基础用户信息
 * <p>
 * 把基础信息一并返回，前端右上角显示"欢迎，王老师"就不用再请求一次 /auth/me。
 * 注意绝不返回密码字段（SysUser.password 上也挂了 @JsonIgnore 双保险）。
 */
@Data
public class LoginVO {

    /** JWT 令牌，前端之后每个请求都带 Authorization: Bearer <token> */
    private String token;

    private Long id;

    private String username;

    private String realName;

    private String role;
}
