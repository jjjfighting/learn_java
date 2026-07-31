package com.studentms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.dto.LoginDTO;
import com.studentms.entity.SysUser;
import com.studentms.mapper.UserMapper;
import com.studentms.util.JwtUtil;
import com.studentms.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务：登录校验 + 令牌签发
 * <p>
 * 有意不拆接口 / 实现两件套：登录是一次性的业务动作，不是对某张表的 CRUD，
 * 用不上 IService 模板，单类 @Service 最直接。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 只引 spring-security-crypto 就能用业界标准的 BCrypt：
     * 每次加密自带随机盐并拼进密文，同一个密码两次加密结果都不同，防彩虹表。
     */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));

        // "用户不存在"和"密码错误"统一返回同一句提示：不帮攻击者确认哪个用户名存在（用户枚举攻击）
        if (user == null || !isPasswordMatch(user, dto.getPassword())) {
            throw new BusinessException(ResultCode.WRONG_CREDENTIALS);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        String token = jwtUtil.generateToken(user);
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        return vo;
    }

    /**
     * 密码比对，兼带历史明文密码的"自愈升级"：
     * 建库脚本里的 admin / t_wang 初始密码是明文 123456，而 BCrypt 密文以 $2a$/$2b$/$2y$ 开头。
     * 首次登录若发现库存的还是明文，比对成功后顺手升级成密文写回——
     * 老用户无感知，数据库里的明文随每次登录逐个消失，不用停机跑迁移脚本。
     */
    private boolean isPasswordMatch(SysUser user, String rawPassword) {
        String stored = user.getPassword();
        if (stored == null) {
            return false;
        }
        boolean isBcrypt = stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
        if (isBcrypt) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        // 明文分支：仅用于兼容初始化数据，比对成功立即升级
        if (!stored.equals(rawPassword)) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
        return true;
    }
}
