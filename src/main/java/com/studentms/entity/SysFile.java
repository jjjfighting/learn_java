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
 * 文件实体类：与数据库 sys_file 表一一映射
 * <p>
 * 只存元数据不存文件本体：originalName / storedName / contentType / size / uploadedBy，
 * 文件字节流躺在磁盘 uploads/ 目录。表名用 sys_file 而非 file——
 * 既避免和 java.io.File 撞名，也避开 MySQL 的 FILE 关键字歧义。
 */
@Data
@TableName("sys_file")
public class SysFile implements Serializable {

    /** 主键：跟随数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上传时的原始文件名（下载 / 展示用；落盘用的是 UUID 名） */
    private String originalName;

    /** 磁盘存储名：UUID（去横线）.扩展名 */
    private String storedName;

    /** 文件 MIME 类型 */
    private String contentType;

    /** 文件大小（字节） */
    private Long size;

    /** 上传人ID（外键 -> sys_user.id，允许为空） */
    private Long uploadedBy;

    /** 上传时间：由 MyMetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除：0未删除 1已删除（yml 全局配置自动生效） */
    @JsonIgnore
    private Integer deleted;
}
