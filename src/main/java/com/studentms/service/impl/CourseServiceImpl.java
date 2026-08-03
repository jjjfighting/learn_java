package com.studentms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.entity.Course;
import com.studentms.entity.Score;
import com.studentms.entity.SysUser;
import com.studentms.mapper.CourseMapper;
import com.studentms.mapper.ScoreMapper;
import com.studentms.mapper.UserMapper;
import com.studentms.service.CourseService;
import com.studentms.vo.CourseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程业务层实现
 * <p>
 * 跨模块依赖依旧只注入对方的 Mapper（UserMapper、ScoreMapper），不碰对方 Service。
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    /** 教师角色常量：与 sys_user.role 的取值约定一致 */
    private static final String ROLE_TEACHER = "TEACHER";

    private final UserMapper userMapper;
    private final ScoreMapper scoreMapper;

    public CourseServiceImpl(UserMapper userMapper, ScoreMapper scoreMapper) {
        this.userMapper = userMapper;
        this.scoreMapper = scoreMapper;
    }

    /** 课程列表读多写少，整表缓存一份，10 分钟后过期 */
    @Cacheable(cacheNames = "courseList", key = "'all'")
    @Override
    public List<CourseVO> listCourses() {
        // 一条 LEFT JOIN SQL 带出所有课程的教师姓名，避免 N+1
        return baseMapper.selectCourseListWithTeacher();
    }

    @CacheEvict(cacheNames = "courseList", allEntries = true)
    @Override
    public void addCourse(Course course) {
        checkTeacher(course.getTeacherId());
        this.save(course);
    }

    /** 改课程名会同时影响统计回显的 courseName，所以课程列表和"按课程统计"缓存一起失效 */
    @CacheEvict(cacheNames = {"courseList", "scoreStatsByCourse"}, allEntries = true)
    @Override
    public void updateCourse(Long id, Course course) {
        if (this.getById(id) == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }
        checkTeacher(course.getTeacherId());
        course.setId(id);
        this.updateById(course);
    }

    @CacheEvict(cacheNames = "courseList", allEntries = true)
    @Override
    public void removeCourse(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }
        // 与"班级下有学生禁删"同构的保护：成绩表外键 fk_score_course 是 RESTRICT，
        // 业务层先给出友好提示，数据库约束留作最后防线
        Long count = scoreMapper.selectCount(
                new LambdaQueryWrapper<Score>().eq(Score::getCourseId, id));
        if (count > 0) {
            throw new BusinessException(ResultCode.COURSE_HAS_SCORES,
                    "该课程已有 " + count + " 条成绩记录，请先处理成绩后再删除");
        }
        this.removeById(id);
    }

    @Override
    public CourseVO getCourse(Long id) {
        Course course = this.getById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }
        CourseVO vo = new CourseVO();
        BeanUtils.copyProperties(course, vo);
        if (course.getTeacherId() != null) {
            SysUser teacher = userMapper.selectById(course.getTeacherId());
            if (teacher != null) {
                vo.setTeacherName(teacher.getRealName());
            }
        }
        return vo;
    }

    /**
     * 校验授课教师：允许不指定（teacherId 为空）；
     * 指定了就必须——① 用户存在（且未被逻辑删除）② 角色是 TEACHER。
     * 别把这道校验当成"可有可无的客气"：外键只能挡住"用户不存在"，
     * 挡不住"把管理员设成授课教师"这种语义错误。
     */
    private void checkTeacher(Long teacherId) {
        if (teacherId == null) {
            return;
        }
        SysUser user = userMapper.selectById(teacherId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "授课教师不存在（ID=" + teacherId + "）");
        }
        if (!ROLE_TEACHER.equals(user.getRole())) {
            throw new BusinessException(ResultCode.TEACHER_ROLE_REQUIRED,
                    "用户 " + user.getUsername() + " 的角色是 " + user.getRole() + "，不是教师");
        }
    }
}
