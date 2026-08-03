package com.studentms.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 学生导入的 Excel 行映射
 * <p>
 * 刻意全用 String 接收：性别、年龄这类列若直接用 Integer 接收，
 * 单元格里写了"abc"会让 EasyExcel 抛类型转换异常、中断整次读取。
 * 用 String 先接住，再在 Service 里逐行手工解析校验——错误能按行收集，而不是整批崩溃。
 */
@Data
public class StudentImportRow {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("年龄")
    private String age;

    @ExcelProperty("班级")
    private String clazzName;
}
