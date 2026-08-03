package com.studentms.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成绩单导出的 Excel 行映射
 * <p>
 * @ExcelProperty 的 value 就是 Excel 表头列名，顺序与字段声明一致；
 * 表头样式在 Controller 里用 HorizontalCellStyleStrategy 统一设置。
 */
@Data
public class ScoreExportRow {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String studentName;

    @ExcelProperty("分数")
    private BigDecimal score;

    @ExcelProperty("考试日期")
    private LocalDate examTime;

    /** 从成绩查询 VO 拷贝需要的四列 */
    public static ScoreExportRow from(ScoreVO vo) {
        ScoreExportRow row = new ScoreExportRow();
        row.setStudentNo(vo.getStudentNo());
        row.setStudentName(vo.getStudentName());
        row.setScore(vo.getScore());
        row.setExamTime(vo.getExamTime());
        return row;
    }
}
