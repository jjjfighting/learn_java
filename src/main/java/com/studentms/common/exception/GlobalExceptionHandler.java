package com.studentms.common.exception;

import com.studentms.common.Result;
import com.studentms.common.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 核心思想：Controller 里不写 try/catch，任何异常抛出来都由这里统一捕获，转成 Result JSON 返回。
 * <p>
 * 原理：@RestControllerAdvice = @ControllerAdvice + @ResponseBody，
 * 基于 AOP 拦截所有 @RestController 方法抛出的异常，每个 @ExceptionHandler 方法负责一种异常类型。
 * 匹配规则：Spring 优先选择参数类型与异常类型"最接近"的处理方法（子类异常优先于父类）。
 * <p>
 * 注意：这里所有方法返回的 HTTP 状态码都是 200，真正的成败由响应体里的 code 表达——
 * 前端只判断 body.code 一种渠道即可，不用再同时处理 HTTP 层和 body 层两套错误。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 自定义业务异常——最常见的情况，返回异常携带的码和提示语
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        // 业务异常属于"预期内的错误"，warn 级别即可，不用打印堆栈刷屏
        log.warn("业务异常：code={}, message={}", e.getResultCode().getCode(), e.getMessage());
        return Result.error(e.getResultCode(), e.getMessage());
    }

    /**
     * 2. @RequestBody + @Valid 校验失败（JSON 请求体的参数校验）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return Result.error(ResultCode.BAD_REQUEST, buildFieldErrorMessage(e.getBindingResult()));
    }

    /**
     * 3. 表单 / Query 参数绑定校验失败
     * （MethodArgumentNotValidException 是 BindException 的子类，会被上面的方法优先匹配走，
     * 这里兜住其余的绑定异常）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        return Result.error(ResultCode.BAD_REQUEST, buildFieldErrorMessage(e.getBindingResult()));
    }

    /**
     * 4. 单个参数校验失败（类上加 @Validated 后，直接在方法参数上写 @NotBlank / @Min 等）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", message);
        return Result.error(ResultCode.BAD_REQUEST, message);
    }

    /**
     * 5. 请求体 JSON 格式非法（前端传了残缺 / 错误的 JSON）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, "请求体格式错误，请检查 JSON 格式");
    }

    /**
     * 6. 唯一键冲突（DuplicateKeyException 是 DataIntegrityViolationException 的子类，
     * Spring 按"最具体优先"匹配到这里）
     * 典型场景：并发下两个请求同时通过了 Service 的查重，后落库的被唯一索引拒绝
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("唯一键冲突：{}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, "数据已存在，请勿重复提交");
    }

    /**
     * 7. 其他数据库约束冲突（外键 / NOT NULL 等）
     * 典型场景：新增学生时 clazzId 填了一个不存在的班级，被外键约束拒绝
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("数据约束冲突：{}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST, "数据不满足约束：关联的班级或账号可能不存在，或字段值非法");
    }

    /**
     * 8. 兜底处理：捕获其他一切异常，保证前端永远收到结构化的 JSON，而不是 Tomcat 的 HTML 错误页
     * 必须放在最后——参数类型 Exception 是所有异常的祖先类，Spring 会优先匹配更具体的类型
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        // 未知异常要打 error 级别 + 完整堆栈，方便在日志里定位问题
        log.error("系统异常", e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 把字段校验错误拼成一句话，如："name: 姓名不能为空; age: 年龄不能小于 0"
     */
    private String buildFieldErrorMessage(BindingResult bindingResult) {
        String message = bindingResult.getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", message);
        return message;
    }
}
