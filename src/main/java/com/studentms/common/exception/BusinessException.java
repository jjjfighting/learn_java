package com.studentms.common.exception;

import com.studentms.common.ResultCode;
import lombok.Getter;

/**
 * 自定义业务异常
 * <p>
 * 用法：业务规则不满足时直接抛出，全局异常处理器会统一转成 Result 返回给前端。
 * <pre>
 * if (studentMapper.selectById(id) == null) {
 *     throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
 * }
 * </pre>
 * 为什么继承 RuntimeException 而不是 Exception？
 * RuntimeException 是非受检异常，抛出时不强制写 try/catch 或 throws 声明，业务代码更干净。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 异常携带的返回码枚举 */
    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /** 枚举的码 + 自定义提示语（提示语会覆盖枚举的默认 message） */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
