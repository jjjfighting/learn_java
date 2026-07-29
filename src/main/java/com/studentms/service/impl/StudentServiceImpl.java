package com.studentms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.dto.StudentQueryDTO;
import com.studentms.entity.Clazz;
import com.studentms.entity.Student;
import com.studentms.mapper.ClazzMapper;
import com.studentms.mapper.StudentMapper;
import com.studentms.service.StudentService;
import com.studentms.vo.StudentVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 学生业务层实现
 * <p>
 * ServiceImpl<StudentMapper, Student> 已经实现了 IService<Student> 的全部通用方法，
 * 内部持有 baseMapper（就是 StudentMapper 的代理对象），我们只写业务逻辑。
 */
@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    // 跨模块回显班级名称：只注入对方的 Mapper，不碰 ClazzService，避免 Service 循环依赖
    // （ClazzServiceImpl 那边也是同样原则，只注入了 StudentMapper）
    private final ClazzMapper clazzMapper;

    public StudentServiceImpl(ClazzMapper clazzMapper) {
        this.clazzMapper = clazzMapper;
    }

    @Override
    public Page<Student> pageStudents(StudentQueryDTO query) {
        // 条件构造器：每行第一个参数是"该条件是否生效"——查询项为空就不拼接，
        // 否则 like '%%' 这种恒真条件会把全表查出来
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                .eq(StringUtils.hasText(query.getStudentNo()), Student::getStudentNo, query.getStudentNo())
                .like(StringUtils.hasText(query.getName()), Student::getName, query.getName())
                .eq(query.getClazzId() != null, Student::getClazzId, query.getClazzId())
                .orderByDesc(Student::getId);
        // 分页拦截器自动改写 SQL 拼 LIMIT，并额外执行一条 COUNT 查总数
        return this.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public void addStudent(Student student) {
        if (existsByStudentNo(student.getStudentNo(), null)) {
            throw new BusinessException(ResultCode.STUDENT_NO_ALREADY_EXISTS,
                    "学号 " + student.getStudentNo() + " 已存在");
        }
        // 插入成功后，自增主键会被 MP 自动回填到 student.id，Controller 可以取出来返回给前端
        this.save(student);
    }

    @Override
    public void updateStudent(Long id, Student student) {
        if (this.getById(id) == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }
        // 查重时排除自己：不然"改了名字但学号没变"会被误判为学号重复
        if (existsByStudentNo(student.getStudentNo(), id)) {
            throw new BusinessException(ResultCode.STUDENT_NO_ALREADY_EXISTS,
                    "学号 " + student.getStudentNo() + " 已存在");
        }
        // id 以路径参数为准，防止请求体里偷塞一个别的 id 造成错位更新
        student.setId(id);
        super.updateById(student);
    }

    @Override
    public void removeStudent(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }
        // 全局逻辑删除：这里实际执行的是 UPDATE student SET deleted=1 WHERE id=?
        // 成绩表 score 配了 ON DELETE CASCADE，但逻辑删除不走数据库 DELETE，成绩记录会保留——属于预期设计
        this.removeById(id);
    }

    @Override
    public StudentVO getStudent(Long id) {
        Student student = this.getById(id);
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }
        StudentVO vo = new StudentVO();
        // 实体 -> VO：同名字段一把拷过去，再补上关联的班级名称
        BeanUtils.copyProperties(student, vo);
        if (student.getClazzId() != null) {
            // clazzId 可能指向已逻辑删除的班级，selectById 自带 deleted=0 过滤，查不到就是 null
            Clazz clazz = clazzMapper.selectById(student.getClazzId());
            if (clazz != null) {
                vo.setClazzName(clazz.getClazzName());
            }
        }
        return vo;
    }

    /**
     * 学号是否已被占用
     *
     * @param studentNo 待检查学号
     * @param excludeId 需要排除的ID：新增时传 null；修改时传当前记录ID（允许学号不变）
     */
    private boolean existsByStudentNo(String studentNo, Long excludeId) {
        // lambdaQuery() 是 IService 提供的链式查询，exists() 只查有没有，不取数据，开销最小
        return this.lambdaQuery()
                .eq(Student::getStudentNo, studentNo)
                .ne(excludeId != null, Student::getId, excludeId)
                .exists();
    }
}
