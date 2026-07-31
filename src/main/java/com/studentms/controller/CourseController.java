package com.studentms.controller;

import com.studentms.annotation.OperationLog;
import com.studentms.annotation.RequireRole;
import com.studentms.common.Result;
import com.studentms.entity.Course;
import com.studentms.service.CourseService;
import com.studentms.vo.CourseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程模块接口（与前两个模块同构的 RESTful 模板）
 * <pre>
 * GET    /courses          列表（回显授课教师）
 * GET    /courses/{id}     详情（回显授课教师）
 * POST   /courses          新增（教师必须是 TEACHER 角色）
 * PUT    /courses/{id}     修改
 * DELETE /courses/{id}     删除（已有成绩时返回 3004；仅 ADMIN 可操作）
 * </pre>
 */
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public Result<List<CourseVO>> list() {
        return Result.success(courseService.listCourses());
    }

    @GetMapping("/{id}")
    public Result<CourseVO> get(@PathVariable Long id) {
        return Result.success(courseService.getCourse(id));
    }

    @OperationLog(module = "course", action = "CREATE")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody Course course) {
        courseService.addCourse(course);
        return Result.success("新增成功", course.getId());
    }

    @OperationLog(module = "course", action = "UPDATE")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Course course) {
        courseService.updateCourse(id, course);
        return Result.success();
    }

    /** 删除是高危操作：@RequireRole 由认证拦截器统一检查，只有 ADMIN 能走到业务逻辑 */
    @OperationLog(module = "course", action = "DELETE")
    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        courseService.removeCourse(id);
        return Result.success();
    }
}
