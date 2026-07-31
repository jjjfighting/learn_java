package com.studentms.vo;

import lombok.Data;

/**
 * 文件上传结果
 * <p>
 * 前端拿到 url 后存进业务表单（如学生头像 photo 字段），
 * 展示时直接 &lt;img src&gt; 即可。
 */
@Data
public class FileVO {

    /** 文件表主键：前端把它存进业务表单（如学生头像 photo 关联），随时可重新下载 */
    private Long id;

    /** 访问路径，如 /files/view/a1b2c3d4.png（内联展示用） */
    private String url;

    /** 上传时的原始文件名（仅展示用，落盘用的是 UUID 名） */
    private String originalName;

    /** 实际存储的文件名 */
    private String storedName;

    /** 文件大小（字节） */
    private Long size;

    /** 文件 MIME 类型 */
    private String contentType;
}
