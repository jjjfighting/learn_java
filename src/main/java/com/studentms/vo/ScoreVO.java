package com.studentms.vo;

import com.studentms.entity.Score;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成绩视图对象：Score 全部字段 + 学生 / 课程名称回显
 * <p>
 * 中间表（关联表）的 VO 比单表更依赖回显——光给前端两个 ID 没有任何可读性。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScoreVO extends Score {

    /** 学生姓名（回显） */
    private String studentName;

    /** 学号（回显） */
    private String studentNo;

    /** 课程名称（回显） */
    private String courseName;
}
