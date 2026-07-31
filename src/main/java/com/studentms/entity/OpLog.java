package com.studentms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体：op_log 表
 * <p>
 * 有意不带 updateTime / deleted 字段——日志是只追加的审计数据：
 * 不会被修改、不参与逻辑删除（yml 的全局逻辑删除也只对含 deleted 字段的实体生效）。
 */
@Data
@TableName("op_log")
public class OpLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID（登录接口调用时拿不到登录态，可为空） */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 模块 */
    private String module;

    /** 动作 */
    private String action;

    /** HTTP 方法 */
    private String method;

    /** 请求路径 */
    private String path;

    /** 客户端 IP */
    private String ip;

    /** 请求参数（已脱敏、截断到 1000 字符） */
    private String params;

    /** 响应业务码 */
    private Integer resultCode;

    /** 异常消息 */
    private String errorMsg;

    /** 接口耗时（毫秒） */
    private Long costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
