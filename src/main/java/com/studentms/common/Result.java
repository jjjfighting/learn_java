package com.studentms.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果类
 * <p>
 * 约定：所有 Controller 接口都返回 Result，前端只需要按一种固定格式解析：
 * <pre>
 * {
 *   "code": 200,          // 业务码，见 ResultCode
 *   "message": "操作成功", // 提示语，可直接展示给用户
 *   "data": { ... }       // 业务数据，失败时通常为 null
 * }
 * </pre>
 *
 * @param <T> data 承载的业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    /** 业务码 */
    private Integer code;

    /** 提示消息 */
    private String message;

    /** 业务数据 */
    private T data;

    // 私有构造：强制通过下面的静态工厂方法创建，保证返回的对象格式统一
    private Result() {
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ========================= 成功 =========================

    /** 成功，不带数据（新增 / 删除 / 修改场景） */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功，带数据（查询场景） */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功，自定义提示语并带数据 */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // ========================= 失败 =========================

    /** 失败：使用枚举里的默认提示语 */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败：枚举的码 + 自定义提示语（如把"用户名已存在"具体化为"用户名 admin 已存在"） */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /** 失败：完全自定义码和提示语（少用，码值应尽量在 ResultCode 中集中管理） */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
