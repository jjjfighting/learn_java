package com.studentms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studentms.entity.Course;
import com.studentms.vo.CourseVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课程数据访问层
 * <p>
 * 列表走自定义 SQL：LEFT JOIN sys_user 把授课教师姓名一起带出来，
 * 写法与 ClazzMapper 的人数统计同宗同源（连表条件放 ON、deleted=0 要手写）。
 */
public interface CourseMapper extends BaseMapper<Course> {

    @Select("""
            SELECT c.id, c.course_name, c.credit, c.teacher_id, c.description,
                   c.create_time, c.update_time,
                   u.real_name AS teacher_name
            FROM course c
            LEFT JOIN sys_user u ON u.id = c.teacher_id AND u.deleted = 0
            WHERE c.deleted = 0
            ORDER BY c.id DESC
            """)
    List<CourseVO> selectCourseListWithTeacher();
}
