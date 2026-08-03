package com.studentms.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.dto.StudentImportRow;
import com.studentms.dto.StudentQueryDTO;
import com.studentms.entity.Clazz;
import com.studentms.entity.Student;
import com.studentms.mapper.ClazzMapper;
import com.studentms.mapper.StudentMapper;
import com.studentms.service.StudentService;
import com.studentms.vo.ImportErrorRowVO;
import com.studentms.vo.StudentImportResultVO;
import com.studentms.vo.StudentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 学生业务层实现
 * <p>
 * ServiceImpl<StudentMapper, Student> 已经实现了 IService<Student> 的全部通用方法，
 * 内部持有 baseMapper（就是 StudentMapper 的代理对象），我们只写业务逻辑。
 */
@Slf4j
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

    @Override
    public StudentImportResultVO importStudents(MultipartFile file) {
        // ① 文件本身校验：空文件 / 非 Excel 后缀
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !(originalName.endsWith(".xlsx") || originalName.endsWith(".xls"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请上传 .xlsx / .xls 格式的 Excel 文件");
        }
        // ② 一次性读完全部数据行（表头按 StudentImportRow 的 @ExcelProperty 自动对齐列）
        List<StudentImportRow> rows;
        try (InputStream in = file.getInputStream()) {
            rows = EasyExcel.read(in).head(StudentImportRow.class).sheet().doReadSync();
        } catch (IOException e) {
            log.error("学生导入：Excel 读取失败：{}", originalName, e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Excel 文件读取失败");
        } catch (RuntimeException e) {
            // 不是真正的 Excel / 结构损坏时 EasyExcel 抛运行时异常，转成 400 而不是兜底 500
            log.error("学生导入：Excel 格式解析失败：{}", originalName, e);
            throw new BusinessException(ResultCode.BAD_REQUEST, "Excel 文件格式不正确，请检查表头是否为：学号/姓名/性别/年龄/班级");
        }
        // ③ 逐行校验 + 落库（部分成功策略：合法行直接 save，错误行收集进结果）
        StudentImportResultVO result = new StudentImportResultVO();
        Set<String> seenNos = new HashSet<>();
        int total = 0;
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            StudentImportRow row = rows.get(i);
            int rowNum = i + 2; // 第 1 行是表头，数据从第 2 行开始
            // 整行为空的行直接跳过、不计数——Excel 表格末尾常有多余的空行，不算错误
            if (isBlankRow(row)) {
                continue;
            }
            total++;
            List<String> problems = new ArrayList<>();
            Student student = validateRow(row, problems);
            // 学号唯一性两道关：库里已有 / 本次文件内重复（字段校验通过才查，避免空学号去查库）
            if (problems.isEmpty()) {
                if (existsByStudentNo(student.getStudentNo(), null)) {
                    problems.add("学号已存在：" + student.getStudentNo());
                } else if (!seenNos.add(student.getStudentNo())) {
                    problems.add("学号在文件内重复：" + student.getStudentNo());
                }
            }
            if (!problems.isEmpty()) {
                result.getErrors().add(new ImportErrorRowVO(rowNum, contentOf(row), String.join("；", problems)));
                continue;
            }
            this.save(student);
            success++;
        }
        result.setTotalRows(total);
        result.setSuccessCount(success);
        result.setFailCount(result.getErrors().size());
        return result;
    }

    /** 一行是否整行为空：用于跳过 Excel 末尾可能存在的空行 */
    private boolean isBlankRow(StudentImportRow row) {
        return !StringUtils.hasText(row.getStudentNo())
                && !StringUtils.hasText(row.getName())
                && !StringUtils.hasText(row.getGender())
                && !StringUtils.hasText(row.getAge())
                && !StringUtils.hasText(row.getClazzName());
    }

    /**
     * 校验单行并解析出可落库的 Student；发现的问题逐个追加到 problems
     * <p>
     * 校验规则：学号必填且不超 20 位、姓名必填且不超 50 位、性别只能男/女(1/0)、
     * 年龄 0~150、班级按名称查表映射 clazzId（班级不存在报错）。
     */
    private Student validateRow(StudentImportRow row, List<String> problems) {
        Student student = new Student();
        String no = trimToNull(row.getStudentNo());
        String name = trimToNull(row.getName());
        String genderRaw = trimToNull(row.getGender());
        String ageRaw = trimToNull(row.getAge());
        String clazzRaw = trimToNull(row.getClazzName());
        // 学号
        if (no == null) {
            problems.add("学号不能为空");
        } else if (no.length() > 20) {
            problems.add("学号不能超过 20 个字符");
        }
        student.setStudentNo(no);
        // 姓名
        if (name == null) {
            problems.add("姓名不能为空");
        } else if (name.length() > 50) {
            problems.add("姓名不能超过 50 个字符");
        }
        student.setName(name);
        // 性别：男/1 -> 1，女/0 -> 0，留空 -> null（数据库默认 1）
        if (genderRaw != null) {
            Integer gender = switch (genderRaw) {
                case "男", "1" -> 1;
                case "女", "0" -> 0;
                default -> null;
            };
            if (gender == null) {
                problems.add("性别只能为 男/女 或 1/0");
            } else {
                student.setGender(gender);
            }
        }
        // 年龄：可选，须为 0~150 的整数
        if (ageRaw != null) {
            try {
                int age = Integer.parseInt(ageRaw);
                if (age < 0 || age > 150) {
                    problems.add("年龄需在 0~150 之间");
                } else {
                    student.setAge(age);
                }
            } catch (NumberFormatException e) {
                problems.add("年龄必须是数字");
            }
        }
        // 班级：按名称查 clazz 表换 clazzId；留空 -> 未分班（clazzId 为 null）
        if (clazzRaw != null) {
            Clazz clazz = clazzMapper.selectOne(
                    new LambdaQueryWrapper<Clazz>().eq(Clazz::getClazzName, clazzRaw));
            if (clazz == null) {
                problems.add("班级不存在：" + clazzRaw);
            } else {
                student.setClazzId(clazz.getId());
            }
        }
        return student;
    }

    /** 行内容摘要（错误回报里给用户看，各列逗号拼接） */
    private String contentOf(StudentImportRow row) {
        return String.join(",", trimToEmpty(row.getStudentNo()), trimToEmpty(row.getName()),
                trimToEmpty(row.getGender()), trimToEmpty(row.getAge()), trimToEmpty(row.getClazzName()));
    }

    private String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
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
