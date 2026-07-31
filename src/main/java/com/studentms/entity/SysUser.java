package com.studentms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体类：与 sys_user 表映射（管理员 / 教师 / 学生三种角色的登录账号）
 * <p>
 * 本步先用于课程模块的"授课教师"关联，登录认证模块（第七步）会在此之上扩展。
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名（唯一） */
    private String username;

    /**
     * 登录密码。
     * 双重保护：@JsonIgnore 保证任何接口都不会把密码序列化出去；
     * 认证模块上线后存储 BCrypt 密文（见 AuthService 的自愈升级）。
     */
    @JsonIgnore
    private String password;

    /** 真实姓名（教师回显用的就是它） */
    private String realName;

    /** 角色：ADMIN 管理员 / TEACHER 教师 / STUDENT 学生 */
    private String role;

    /** 账号状态：0禁用 1启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @JsonIgnore
    private Integer deleted;
}
