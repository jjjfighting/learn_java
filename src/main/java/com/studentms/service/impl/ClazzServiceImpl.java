package com.studentms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.entity.Clazz;
import com.studentms.entity.Student;
import com.studentms.mapper.ClazzMapper;
import com.studentms.mapper.StudentMapper;
import com.studentms.service.ClazzService;
import com.studentms.vo.ClazzVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 班级业务层实现
 * <p>
 * 注意这里注入的是 StudentMapper 而不是 StudentService：
 * 跨模块只依赖对方的 Mapper，可以避免 Service 之间绕成循环依赖
 * （学生模块的 StudentServiceImpl 反过来也注入了 ClazzMapper，两边都不碰对方的 Service）。
 */
@Service
public class ClazzServiceImpl extends ServiceImpl<ClazzMapper, Clazz> implements ClazzService {

    private final StudentMapper studentMapper;

    public ClazzServiceImpl(StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    /** 班级列表读多写少，整表缓存一份；注意它含"在校学生数"，所以学生变动也要失效它 */
    @Cacheable(cacheNames = "clazzList", key = "'all'")
    @Override
    public List<ClazzVO> listClazzes() {
        // 走自定义 SQL：LEFT JOIN student 分组统计，一次查询拿到全部班级 + 人数
        return baseMapper.selectClazzListWithStudentCount();
    }

    @CacheEvict(cacheNames = "clazzList", allEntries = true)
    @Override
    public void addClazz(Clazz clazz) {
        this.save(clazz);
    }

    @CacheEvict(cacheNames = "clazzList", allEntries = true)
    @Override
    public void updateClazz(Long id, Clazz clazz) {
        if (this.getById(id) == null) {
            throw new BusinessException(ResultCode.CLAZZ_NOT_FOUND);
        }
        clazz.setId(id);
        this.updateById(clazz);
    }

    @CacheEvict(cacheNames = "clazzList", allEntries = true)
    @Override
    public void removeClazz(Long id) {
        //  12321
        if (this.getById(id) == null) {
            throw new BusinessException(ResultCode.CLAZZ_NOT_FOUND);
        }
        // 删除前业务校验：统计该班级下的"在校学生"。
        // selectCount 是 MP 生成的 SQL，会自动追加 AND deleted=0，逻辑删除的学生不计入
        Long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getClazzId, id));
        if (count > 0) {
            // 提示语带上具体人数，前端可直接展示
            throw new BusinessException(ResultCode.CLAZZ_HAS_STUDENTS,
                    "该班级下还有 " + count + " 名学生，请先转移学生后再删除");
        }
        this.removeById(id);
        // 兜底：万一并发场景下校验后又有学生被划进来，数据库外键约束
        // fk_student_clazz (RESTRICT) 会拒绝物理删除，异常由全局处理器转成 400
    }

    @Override
    public Clazz getClazz(Long id) {
        Clazz clazz = this.getById(id);
        if (clazz == null) {
            throw new BusinessException(ResultCode.CLAZZ_NOT_FOUND);
        }
        return clazz;
    }
}
