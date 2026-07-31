package com.studentms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 课程维度成绩统计：GET /scores/stats/by-course?courseId=1 的返回形状
 */
@Data
public class CourseScoreStatsVO {

    private Long courseId;

    private String courseName;

    /** 已判分人数 */
    private Integer examCount;

    /** 平均分（保留两位小数；暂无成绩时为 null） */
    private BigDecimal avgScore;

    /** 最高分 */
    private BigDecimal maxScore;

    /** 最低分 */
    private BigDecimal minScore;
}
