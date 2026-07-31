package com.studentms.vo;

import com.studentms.entity.Course;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程视图对象：Course 全部字段 + 授课教师姓名回显
 * <p>
 * 与学生详情回显班级名（StudentVO）完全同构——关联名称回显是详情页的通用套路。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseVO extends Course {

    /** 授课教师姓名（sys_user.real_name）；未指定教师或教师已删除时为 null */
    private String teacherName;
}
