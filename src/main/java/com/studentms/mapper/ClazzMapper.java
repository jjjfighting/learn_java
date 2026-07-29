package com.studentms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studentms.entity.Clazz;
import com.studentms.vo.ClazzVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 班级数据访问层
 * <p>
 * BaseMapper 负责单表 CRUD；带"学生人数统计"的列表是跨表查询，
 * 用 @Select 注解写自定义 SQL——这是单表操作之外最常用的扩展方式。
 */
public interface ClazzMapper extends BaseMapper<Clazz> {

    /**
     * 查询班级列表，附带每个班级的在校学生数
     * <p>
     * 三个要点：
     * 1. LEFT JOIN 保证没有学生的班级也会出现在结果里（人数为 0），用 INNER JOIN 会把它们丢掉；
     * 2. ⚠️ 自定义 SQL 不会自动拼接逻辑删除条件！c.deleted=0 和 s.deleted=0 都必须手写，
     *    尤其 s.deleted=0 要写在 ON 后面而不是 WHERE 里——写在 WHERE 会把"没有学生的班级"也过滤掉，
     *    LEFT JOIN 就退化成 INNER JOIN 了；
     * 3. GROUP BY c.id 即可：MySQL8 开启的 ONLY_FULL_GROUP_BY 允许按主键分组后
     *    直接 SELECT 该表的其他列（函数依赖），不用把每一列都列进 GROUP BY。
     */
    @Select("""
            SELECT c.id, c.clazz_name, c.grade, c.head_teacher, c.description,
                   c.create_time, c.update_time,
                   COUNT(s.id) AS student_count
            FROM clazz c
            LEFT JOIN student s ON s.clazz_id = c.id AND s.deleted = 0
            WHERE c.deleted = 0
            GROUP BY c.id
            ORDER BY c.id DESC
            """)
    List<ClazzVO> selectClazzListWithStudentCount();
}
