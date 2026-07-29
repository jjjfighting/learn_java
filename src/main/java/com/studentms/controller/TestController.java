package com.studentms.controller;

import com.studentms.common.Result;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器：仅用于验证公共层是否生效，后续模块开发完成后可以整体删除
 * <p>
 * 四个接口对应四种返回情况：
 * 1. GET  /test/success        正常成功
 * 2. GET  /test/business-error 业务异常（BusinessException -> 全局处理器）
 * 3. POST /test/validate       参数校验失败（@Valid -> 全局处理器）
 * 4. GET  /test/unknown-error  未知异常（兜底处理器）
 */
@RestController
@RequestMapping("/test")
public class TestController {

    /** 场景一：正常返回带数据的成功结果 */
    @GetMapping("/success")
    public Result<Map<String, Object>> success() {
        Map<String, Object> data = new HashMap<>();
        data.put("project", "学生管理系统");
        data.put("step", "公共层搭建");
        return Result.success(data);
    }

    /** 场景二：抛出业务异常，由全局处理器转成 {"code":2001,"message":"学生不存在"} */
    @GetMapping("/business-error")
    public Result<Void> businessError() {
        // 真实业务里类似：查不到学生就抛异常，而不是返回 null 让调用方猜
        throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
    }

    /** 场景三：请求体 JSON 参数校验，传 {} 会收到 {"code":400,"message":"name: 姓名不能为空"} */
    @PostMapping("/validate")
    public Result<Void> validate(@Valid @RequestBody TestParam param) {
        return Result.success();
    }

    /** 场景四：抛出未预期的异常，由兜底处理器转成 {"code":500,...}，控制台会打出完整堆栈 */
    @GetMapping("/unknown-error")
    public Result<Void> unknownError() {
        throw new RuntimeException("模拟一个没有预料到的错误");
    }

    /** 校验用的入参对象 */
    @Data
    public static class TestParam {

        @NotBlank(message = "姓名不能为空")
        private String name;

        @Min(value = 0, message = "年龄不能小于 0")
        private Integer age;
    }
}
