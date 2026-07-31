package com.studentms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 学生维度成绩统计：GET /scores/stats/by-student?studentId=1 的返回形状
 * <p>
 * 全部由 SQL 聚合函数算好后直接映射，Java 侧不做二次计算——
 * 能在数据库算完的就别搬到内存里算。
 */
@Data
public class StudentScoreStatsVO {

    private Long studentId;

    private String studentName;

    /** 已判分科目数（score 为 null 的记录不计入） */
    private Integer courseCount;

    /** 平均分（保留两位小数；暂无成绩时为 null） */
    private BigDecimal avgScore;

    /** 最高分 */
    private BigDecimal maxScore;

    /** 总分 */
    private BigDecimal totalScore;
}
