package com.studentms.vo;

import com.studentms.entity.Student;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生详情视图对象：Student 全部字段 + 班级名称回显
 * <p>
 * 前端展示学生详情时想直接看到"2023级软件1班"，而不是一个 clazzId=1 让人自己去查。
 * 这种"关联名称回显"是详情页最常见的需求，用 VO 承接最合适。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StudentVO extends Student {

    /** 班级名称（回显）；未分班或班级已删除时为 null */
    private String clazzName;
}
