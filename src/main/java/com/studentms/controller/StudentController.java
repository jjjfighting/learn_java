package com.studentms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studentms.common.Result;
import com.studentms.dto.StudentQueryDTO;
import com.studentms.entity.Student;
import com.studentms.service.StudentService;
import com.studentms.vo.StudentVO;
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

/**
 * 学生模块接口（RESTful 风格：URL 只表示资源，动作交给 HTTP 方法）
 * <pre>
 * GET    /students          分页条件查询
 * GET    /students/{id}     查询详情
 * POST   /students          新增
 * PUT    /students/{id}     修改
 * DELETE /students/{id}     删除
 * </pre>
 */
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    // @RequiredArgsConstructor 让 Lombok 为 final 字段生成构造器，实现"构造器注入"——
    // 官方推荐写法：依赖不可变、便于单元测试、缺依赖时启动直接报错而不是运行时 NPE
    private final StudentService studentService;

    /** 分页条件查询，例：GET /students?name=张&pageNum=1&pageSize=10 */
    @GetMapping
    public Result<Page<Student>> page(@Valid StudentQueryDTO query) {
        return Result.success(studentService.pageStudents(query));
    }

    /** 查询详情（回显班级名称），例：GET /students/1 */
    @GetMapping("/{id}")
    public Result<StudentVO> get(@PathVariable Long id) {
        return Result.success(studentService.getStudent(id));
    }

    /** 新增学生，请求体为 Student JSON */
    @PostMapping
    public Result<Long> add(@Valid @RequestBody Student student) {
        studentService.addStudent(student);
        // save 后自增主键已回填到 student.id，返回它方便前端直接跳详情页
        return Result.success("新增成功", student.getId());
    }

    /** 修改学生，id 以路径参数为准 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Student student) {
        studentService.updateStudent(id, student);
        return Result.success();
    }

    /** 删除学生（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        studentService.removeStudent(id);
        return Result.success();
    }
}
