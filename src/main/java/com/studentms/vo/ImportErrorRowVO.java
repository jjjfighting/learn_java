package com.studentms.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入失败的一行：行号 + 原始内容摘要 + 错误原因
 * <p>
 * rowNum 从 2 开始（第 1 行是表头），方便用户照着 Excel 定位到具体行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportErrorRowVO {

    /** 出错行在 Excel 中的行号（第 1 行是表头，数据从第 2 行起） */
    private Integer rowNum;

    /** 该行原始内容摘要（各列用逗号拼接） */
    private String content;

    /** 错误原因，可能有多个（分号拼接） */
    private String reason;
}
