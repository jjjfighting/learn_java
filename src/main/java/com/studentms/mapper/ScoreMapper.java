package com.studentms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studentms.entity.Score;
import com.studentms.vo.CourseScoreStatsVO;
import com.studentms.vo.ScoreVO;
import com.studentms.vo.StudentScoreStatsVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 成绩数据访问层
 * <p>
 * 本项目的"自定义 SQL 集中展示区"：动态条件、多表 JOIN 回显、聚合统计全在这里。
 */
public interface ScoreMapper extends BaseMapper<Score> {

    /**
     * 按学生 / 按课程两个维度查成绩列表（JOIN 回显学生、课程信息）
     * <p>
     * 第一次使用 &lt;script&gt; 动态 SQL：@Select 里嵌 MyBatis XML 标签，
     * &lt;if&gt; 按参数是否为 null 决定拼不拼条件——效果等价于 LambdaQueryWrapper 的条件构造，
     * 但跨表 JOIN 场景只能靠它（条件构造器管不了 JOIN）。
     */
    @Select("""
            <script>
            SELECT s.id, s.student_id, s.course_id, s.score, s.exam_time, s.create_time, s.update_time,
                   st.name AS student_name, st.student_no AS student_no,
                   c.course_name AS course_name
            FROM score s
            LEFT JOIN student st ON st.id = s.student_id AND st.deleted = 0
            LEFT JOIN course c  ON c.id  = s.course_id  AND c.deleted = 0
            WHERE s.deleted = 0
            <if test="studentId != null"> AND s.student_id = #{studentId} </if>
            <if test="courseId  != null"> AND s.course_id  = #{courseId} </if>
            ORDER BY s.id DESC
            </script>
            """)
    List<ScoreVO> selectScoreList(@Param("studentId") Long studentId,
                                  @Param("courseId") Long courseId);

    /** 成绩详情（同样 JOIN 回显姓名与课程名） */
    @Select("""
            SELECT s.id, s.student_id, s.course_id, s.score, s.exam_time, s.create_time, s.update_time,
                   st.name AS student_name, st.student_no AS student_no,
                   c.course_name AS course_name
            FROM score s
            LEFT JOIN student st ON st.id = s.student_id AND st.deleted = 0
            LEFT JOIN course c  ON c.id  = s.course_id  AND c.deleted = 0
            WHERE s.deleted = 0 AND s.id = #{id}
            """)
    ScoreVO selectScoreById(@Param("id") Long id);

    /**
     * 按学生维度统计：科目数 / 平均分 / 最高分 / 总分
     * <p>
     * 聚合查询不带 GROUP BY 时永远返回"恰好一行"：没有成绩记录时 course_count=0、
     * 平均/最高为 null——Service 层据此判断要不要提示"暂无成绩"。
     * COUNT(s.score) 而不是 COUNT(*)：score 允许为 null（先建记录后判分），
     * COUNT(列) 不数 null 行，统计的才是"已判分科目数"。
     */
    @Select("""
            SELECT s.student_id,
                   st.name AS student_name,
                   COUNT(s.score) AS course_count,
                   ROUND(AVG(s.score), 2) AS avg_score,
                   MAX(s.score) AS max_score,
                   SUM(s.score) AS total_score
            FROM score s
            LEFT JOIN student st ON st.id = s.student_id AND st.deleted = 0
            WHERE s.deleted = 0 AND s.student_id = #{studentId}
            """)
    StudentScoreStatsVO statsByStudent(@Param("studentId") Long studentId);

    /** 按课程维度统计：参考人数 / 平均分 / 最高分 / 最低分 */
    @Select("""
            SELECT s.course_id,
                   c.course_name AS course_name,
                   COUNT(s.score) AS exam_count,
                   ROUND(AVG(s.score), 2) AS avg_score,
                   MAX(s.score) AS max_score,
                   MIN(s.score) AS min_score
            FROM score s
            LEFT JOIN course c ON c.id = s.course_id AND c.deleted = 0
            WHERE s.deleted = 0 AND s.course_id = #{courseId}
            """)
    CourseScoreStatsVO statsByCourse(@Param("courseId") Long courseId);

    /**
     * 物理删除 (学生, 课程) 组合下的逻辑删除"墓碑"记录（deleted=1）
     * <p>
     * 为什么需要它：联合唯一键 uk_student_course 建在物理行上，逻辑删除的行依然占着坑——
     * 删掉一条成绩后再给同一学生同一课程录分，会被唯一键拒绝。
     * 所以重新录入前先清掉墓碑，详见 ScoreServiceImpl#addScore。
     */
    @Delete("DELETE FROM score WHERE student_id = #{studentId} AND course_id = #{courseId} AND deleted = 1")
    int physicallyDeleteTombstone(@Param("studentId") Long studentId,
                                  @Param("courseId") Long courseId);
}
