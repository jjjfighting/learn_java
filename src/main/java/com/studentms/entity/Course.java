package com.studentms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程实体类：与 course 表映射
 */
@Data
@TableName("course")
public class Course implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程名称 */
    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    /** 学分，如 3.5。数据库是 DECIMAL(3,1)，Java 侧对应 BigDecimal——浮点 double 有精度误差，钱和分数这类数值永远用 BigDecimal */
    @DecimalMin(value = "0.0", message = "学分不能为负")
    @Digits(integer = 2, fraction = 1, message = "学分格式如 3.5（最多两位整数、一位小数）")
    private BigDecimal credit;

    /** 授课教师ID（外键 -> sys_user.id，且该用户角色必须是 TEACHER） */
    private Long teacherId;

    /** 课程简介 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @JsonIgnore
    private Integer deleted;
}
