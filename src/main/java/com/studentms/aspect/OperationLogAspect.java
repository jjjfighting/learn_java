package com.studentms.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentms.annotation.OperationLog;
import com.studentms.common.Result;
import com.studentms.common.ResultCode;
import com.studentms.common.UserContext;
import com.studentms.common.UserInfo;
import com.studentms.common.exception.BusinessException;
import com.studentms.entity.OpLog;
import com.studentms.mapper.OpLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 操作日志切面：环绕所有标注 @OperationLog 的方法
 * <p>
 * AOP 术语对照：
 * - 切面（Aspect）= 本类：横切关注点的模块化封装
 * - 连接点（JoinPoint）= 每一次被拦截的方法调用
 * - 切点（Pointcut）= @annotation(OperationLog)：定义"拦哪些"
 * - 通知（Advice）= @Around 方法：定义"拦下来干什么"，环绕通知能控制目标方法执不执行
 * <p>
 * 执行模型：
 * <pre>
 * 请求 -> 切面前置记录 -> pjp.proceed() 执行原方法 -> 后置收集结果/异常 -> 写日志 -> 响应
 * </pre>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OpLogMapper opLogMapper;
    private final ObjectMapper objectMapper;

    /** 参数序列化上限，防止有人传巨大 JSON 撑爆日志表 */
    private static final int PARAMS_MAX_LENGTH = 1000;

    @Around(value = "@annotation(operationLog)", argNames = "pjp,operationLog")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        OpLog record = new OpLog();
        record.setModule(operationLog.module());
        record.setAction(operationLog.action());

        // 操作人：拦截器验完 token 存在 ThreadLocal 里，这里直接取。
        // 登录接口在白名单内没有登录态——username 稍后从请求参数里兜底提取
        UserInfo user = UserContext.get();
        if (user != null) {
            record.setUserId(user.getId());
            record.setUsername(user.getUsername());
        }

        // 请求信息：切面不在 Controller 方法签名里，靠 RequestContextHolder 拿当前请求
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            record.setMethod(request.getMethod());
            record.setPath(request.getRequestURI());
            record.setIp(resolveIp(request));
        }
        record.setParams(buildParams(pjp.getArgs()));

        Object result = null;
        try {
            result = pjp.proceed();      // 放行，执行原方法
            return result;
        } catch (Throwable e) {
            // 业务异常记它的码，未知异常记 500；异常原样抛出，切面绝不吞异常
            if (e instanceof BusinessException be) {
                record.setResultCode(be.getResultCode().getCode());
            } else {
                record.setResultCode(ResultCode.INTERNAL_SERVER_ERROR.getCode());
            }
            record.setErrorMsg(truncate(e.getMessage(), 500));
            throw e;
        } finally {
            // 正常返回时从 Result 里取业务码（全局异常处理器包装的也逃不掉，因为异常已在上一个 catch 处理）
            if (result instanceof Result<?> r) {
                record.setResultCode(r.getCode());
            }
            if (record.getUsername() == null) {
                record.setUsername(extractUsername(record.getParams()));
            }
            record.setCostMs(System.currentTimeMillis() - start);
            try {
                opLogMapper.insert(record);
            } catch (Exception e) {
                // 日志写失败绝不能影响业务：只记错误日志，不抛
                log.error("操作日志写入失败：{}", e.getMessage(), e);
            }
        }
    }

    /**
     * 序列化方法参数为 JSON：
     * - MultipartFile 替换成占位符（序列化二进制没意义还会炸）
     * - password 字段脱敏成 ***
     * - 超长截断
     */
    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            Object[] safe = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof MultipartFile f) {
                    safe[i] = "[file: " + f.getOriginalFilename() + ", " + f.getSize() + " bytes]";
                } else if (args[i] instanceof HttpServletRequest) {
                    safe[i] = "[request]";   // servlet 对象不可序列化
                } else {
                    safe[i] = args[i];
                }
            }
            String json = objectMapper.writeValueAsString(safe);
            // 脱敏：覆盖 "password":"任意值" -> "password":"***"
            json = json.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
            return truncate(json, PARAMS_MAX_LENGTH);
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
    }

    /** 从已脱敏的参数 JSON 里提取 username（登录接口专用兜底） */
    private String extractUsername(String params) {
        if (params == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(params);
            for (JsonNode node : root) {
                JsonNode username = node.get("username");
                if (username != null && !username.isNull()) {
                    return username.asText();
                }
            }
        } catch (Exception ignored) {
            // 提取不到就算了，日志 username 允许为空
        }
        return null;
    }

    /** 客户端真实 IP：优先读反向代理写入的 X-Forwarded-For 第一段 */
    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
