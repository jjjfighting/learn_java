package com.studentms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.studentms.dto.StudentQueryDTO;
import com.studentms.entity.Student;
import com.studentms.vo.StudentImportResultVO;
import com.studentms.vo.StudentVO;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 从 Excel 批量导入学生（部分成功策略）
     * <p>
     * 逐行校验（学号必填且唯一、姓名必填、性别/年龄/班级合法性），
     * 合法行落库，错误行收集进返回结果，不会中断整批导入。
     */
    StudentImportResultVO importStudents(MultipartFile file);
}
