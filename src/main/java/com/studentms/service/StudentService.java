package com.studentms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.studentms.dto.StudentQueryDTO;
import com.studentms.entity.Student;
import com.studentms.vo.StudentVO;

/**
 * 学生业务层接口
 * <p>
 * 继承 IService<Student>：通用单表操作（save / getById / updateById / removeById / page ...）
 * 全部由框架提供，接口里只声明"带业务逻辑"的方法。
 */
public interface StudentService extends IService<Student> {

    /** 分页条件查询 */
    Page<Student> pageStudents(StudentQueryDTO query);

    /** 新增学生（含学号唯一性校验） */
    void addStudent(Student student);

    /** 修改学生（含存在性校验 + 学号排除自身查重） */
    void updateStudent(Long id, Student student);

    /** 删除学生（含存在性校验；全局逻辑删除，实际执行 UPDATE deleted=1） */
    void removeStudent(Long id);

    /** 查询详情（含班级名称回显），不存在时抛业务异常 */
    StudentVO getStudent(Long id);
}
