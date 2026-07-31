package com.studentms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.studentms.entity.Course;
import com.studentms.vo.CourseVO;

import java.util.List;

/**
 * 课程业务层接口（业务模块收尾之作，模板与学生 / 班级完全同构）
 */
public interface CourseService extends IService<Course> {

    /** 课程列表（回显授课教师姓名） */
    List<CourseVO> listCourses();

    /** 新增课程（授课教师必须存在且角色为 TEACHER） */
    void addCourse(Course course);

    /** 修改课程（同样校验授课教师） */
    void updateCourse(Long id, Course course);

    /** 删除课程（已有成绩记录时禁止删除） */
    void removeCourse(Long id);

    /** 课程详情（回显授课教师姓名），不存在时抛 COURSE_NOT_FOUND */
    CourseVO getCourse(Long id);
}
