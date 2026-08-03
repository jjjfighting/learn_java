package com.studentms.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生导入结果：本次导入的统计 + 失败明细
 * <p>
 * 采用"部分成功"策略：合法行直接落库，错误行收集在这里返回给前端展示。
 */
@Data
public class StudentImportResultVO {

    /** 有效数据总行数（不含表头，也不含整行为空的行） */
    private Integer totalRows;

    /** 成功落库的行数 */
    private Integer successCount;

    /** 校验失败的行数 */
    private Integer failCount;

    /** 失败行明细（行号 / 内容 / 原因） */
    private List<ImportErrorRowVO> errors = new ArrayList<>();
}
