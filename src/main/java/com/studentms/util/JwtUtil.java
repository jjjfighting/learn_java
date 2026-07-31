package com.studentms.util;

import com.studentms.common.UserInfo;
import com.studentms.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：负责令牌的签发与解析
 * <p>
 * JWT 结构 = header.payload.signature：
 * - payload 里放用户身份（用户名、角色等 claims），Base64 编码但**不加密**——别往里塞密码等敏感信息；
 * - signature 用服务端密钥对前两段签名，篡改 payload 必然验签失败——这就是令牌"不可伪造"的原理。
 * <p>
 * 服务端不存 session，只验签名——这是 JWT 无状态认证的核心，也是它适合前后端分离的原因。
 */
@Component
public class JwtUtil {

    @Value("${auth.jwt.secret}")
    private String secret;

    @Value("${auth.jwt.expire-minutes}")
    private long expireMinutes;

    /** HS256 要求密钥至少 256 位，直接用配置的字符串生成密钥对象 */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 登录成功后签发令牌：把身份关键信息写进 claims */
    public String generateToken(SysUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("realName", user.getRealName())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMinutes * 60 * 1000))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析并验签：签名不对或已过期都会抛 JwtException，由调用方统一按"未登录"处理
     */
    public UserInfo parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new UserInfo(
                claims.get("userId", Long.class),
                claims.getSubject(),
                claims.get("realName", String.class),
                claims.get("role", String.class));
    }
}
