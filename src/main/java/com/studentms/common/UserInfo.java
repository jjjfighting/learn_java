package com.studentms.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户的精简画像：JWT claims 与业务代码之间的载体
 * <p>
 * 只带鉴权需要的字段（id / 用户名 / 姓名 / 角色），不是 SysUser 实体——
 * 令牌里的信息越精简越好，反正改了密码、禁用了账号可以靠过期时间自然失效。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private Long id;

    private String username;

    private String realName;

    /** ADMIN / TEACHER / STUDENT */
    private String role;
}
