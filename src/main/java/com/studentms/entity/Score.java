package com.studentms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成绩实体类：student + course 的中间表（多对多拆解）
 * <p>
 * 表上有联合唯一键 uk_student_course (student_id, course_id)：
 * 一名学生一门课只允许一条成绩，重复录入会被数据库拒绝（Service 层也会提前拦）。
 */
@Data
@TableName("score")
public class Score implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID（外键 -> student.id，删学生时数据库级联删除）。必填性在 Service 层校验——新增必填、修改不动它，注解做不到这种差异化 */
    private Long studentId;

    /** 课程ID（外键 -> course.id，RESTRICT：有成绩时禁止删课程） */
    private Long courseId;

    /** 分数，如：92.50。允许为空（先建记录后判分），填了就必须非负、且符合 DECIMAL(5,2) 的位数 */
    @DecimalMin(value = "0.00", message = "分数不能为负")
    @Digits(integer = 3, fraction = 2, message = "分数格式如 92.50（最多三位整数、两位小数）")
    private BigDecimal score;

    /** 考试日期 */
    private LocalDate examTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @JsonIgnore
    private Integer deleted;
}
