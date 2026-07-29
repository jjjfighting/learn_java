package com.studentms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生实体类：与数据库 student 表一一映射
 * <p>
 * 映射规则：类名 Student -> 表名 student、属性名 studentNo -> 列名 student_no，
 * 分别由 @TableName 和 yml 里的 map-underscore-to-camel-case 完成。
 */
@Data
@TableName("student")
public class Student implements Serializable {

    /** 主键：跟随数据库自增（与 yml 全局的 id-type: auto 一致，这里显式写出来更清晰） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号（唯一业务编号），新增 / 修改时必填 */
    @NotBlank(message = "学号不能为空")
    private String studentNo;

    /** 学生姓名，新增 / 修改时必填 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 性别：0女 1男（允许不传，数据库默认 1；传了就必须是 0 或 1） */
    @Min(value = 0, message = "性别只能是 0(女) 或 1(男)")
    @Max(value = 1, message = "性别只能是 0(女) 或 1(男)")
    private Integer gender;

    /** 年龄 */
//    @NotNull(message = "age不能为空")
    @Min(value = 0, message = "年龄不能小于 0")
    @Max(value = 150, message = "年龄不能大于 150")
    private Integer age;

    /** 联系电话 */
    private String phone;

    /** 电子邮箱：允许为空，填了就必须符合邮箱格式 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 所属班级ID（外键 -> clazz.id） */
    private Long clazzId;

    /** 关联登录账号ID（外键 -> sys_user.id，允许为空） */
    private Long userId;

    /** 创建时间：新增时由 MyMetaObjectHandler 自动填充，业务代码不用管 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间：新增、更新时都由 MyMetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0未删除 1已删除。不用写 @TableLogic——yml 里已全局开启（logic-delete-field: deleted）。
     * @JsonIgnore：内部实现细节，不序列化进响应 JSON，前端看不到这个字段
     */
    @JsonIgnore
    private Integer deleted;
}
