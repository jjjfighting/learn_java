package com.studentms.vo;

import com.studentms.entity.Clazz;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 班级列表视图对象（View Object）：Clazz 全部字段 + 学生人数
 * <p>
 * VO 是"展示层专用模型"：数据库表长什么样是实体类的事，页面需要什么形状是 VO 的事，
 * 两者解耦，统计字段才不会污染实体类。
 * <p>
 * studentCount 由 ClazzMapper 自定义 SQL 里的 COUNT(s.id) AS student_count 映射而来
 * （下划线转驼峰由 yml 的 map-underscore-to-camel-case 完成）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClazzVO extends Clazz {

    /** 在校学生数（逻辑删除的学生不计入） */
    private Integer studentCount;
}
