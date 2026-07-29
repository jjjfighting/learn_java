package com.studentms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 学生分页查询条件对象：承接 GET /students 的查询参数
 * <p>
 * 为什么不直接接收一堆 @RequestParam？参数一多方法签名会很难看，
 * 用一个对象打包既整洁，还能在字段上挂校验注解。DTO = Data Transfer Object，数据传输对象。
 */
@Data
public class StudentQueryDTO {

    /** 学号：精确匹配 */
    private String studentNo;

    /** 姓名：模糊匹配（like '%xxx%'） */
    private String name;

    /** 班级ID：精确匹配 */
    private Long clazzId;

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码从 1 开始")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 10;
}
