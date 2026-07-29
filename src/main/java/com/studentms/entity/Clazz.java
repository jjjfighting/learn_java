package com.studentms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级实体类：与数据库 clazz 表一一映射
 * <p>
 * 表名为什么是 clazz 而不是 class？因为 class 是 Java 关键字，类名不能用，
 * 干脆表名、类名、URL 统一用 clazz，省得记两套名字。
 */
@Data
@TableName("clazz")
public class Clazz implements Serializable {

    /** 主键：跟随数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级名称，如：2023级软件1班 */
    @NotBlank(message = "班级名称不能为空")
    private String clazzName;

    /** 入学年级，如：2023 */
    private String grade;

    /** 班主任姓名 */
    private String headTeacher;

    /** 班级简介 */
    private String description;

    /** 创建时间：新增时由 MyMetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间：新增、更新时都自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：yml 全局开启，@JsonIgnore 不暴露给前端 */
    @JsonIgnore
    private Integer deleted;
}
