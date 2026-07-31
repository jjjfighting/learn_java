package com.studentms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.entity.Course;
import com.studentms.entity.Score;
import com.studentms.entity.Student;
import com.studentms.mapper.CourseMapper;
import com.studentms.mapper.ScoreMapper;
import com.studentms.mapper.StudentMapper;
import com.studentms.service.ScoreService;
import com.studentms.vo.CourseScoreStatsVO;
import com.studentms.vo.ScoreVO;
import com.studentms.vo.StudentScoreStatsVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 成绩业务层实现
 * <p>
 * 跨模块依赖依旧只注入 Mapper（StudentMapper、CourseMapper），不碰对方 Service。
 */
@Service
public class ScoreServiceImpl extends ServiceImpl<ScoreMapper, Score> implements ScoreService {

    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;

    public ScoreServiceImpl(StudentMapper studentMapper, CourseMapper courseMapper) {
        this.studentMapper = studentMapper;
        this.courseMapper = courseMapper;
    }

    @Override
    public List<ScoreVO> listScores(Long studentId, Long courseId) {
        return baseMapper.selectScoreList(studentId, courseId);
    }

    @Override
    public void addScore(Score score) {
        // 录入前四道关，缺一不可：
        // ① studentId / courseId 必填（实体注解做不到"新增必填、修改不需要"的差异化，放业务层）
        if (score.getStudentId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "学生ID不能为空");
        }
        if (score.getCourseId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "课程ID不能为空");
        }
        // ② 学生和课程必须存在（selectById 自带 deleted=0，逻辑删除的也算不存在）
        Student student = studentMapper.selectById(score.getStudentId());
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND, "待录入成绩的学生不存在");
        }
        Course course = courseMapper.selectById(score.getCourseId());
        if (course == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND, "待录入成绩的课程不存在");
        }
        // ③ 清理墓碑：联合唯一键 uk_student_course 约束的是物理行，
        //    "删除成绩"只是 UPDATE deleted=1，旧行依然占着唯一索引的坑。
        //    不清理的话，同一学生同一课程"删了再录"会被数据库误杀。
        //    （这一步只在有墓碑时才真删东西，平时无副作用）
        baseMapper.physicallyDeleteTombstone(score.getStudentId(), score.getCourseId());
        // ④ 在库重复校验：拦截"同一学生同一课程已有有效成绩"
        boolean exists = this.lambdaQuery()
                .eq(Score::getStudentId, score.getStudentId())
                .eq(Score::getCourseId, score.getCourseId())
                .exists();
        if (exists) {
            throw new BusinessException(ResultCode.SCORE_ALREADY_EXISTS,
                    "学生 " + student.getName() + " 在《" + course.getCourseName() + "》已有成绩，请使用修改接口");
        }
        // ⑤ 并发兜底：两个请求同时穿过第④关时，数据库唯一索引拒绝后落库的那个，
        //    异常由全局处理器的 DuplicateKeyException 分支接住转 400
        this.save(score);
    }

    @Override
    public void updateScore(Long id, Score score) {
        Score db = this.getBaseMapper().selectById(id);
        if (db == null) {
            throw new BusinessException(ResultCode.SCORE_NOT_FOUND);
        }
        // 成绩记录由"谁 + 哪门课"确定，修改只改分数和考试日期；
        // 请求体里就算带了 studentId/courseId 也一律忽略（从库记录走），防止改归属造成错位
        db.setScore(score.getScore());
        db.setExamTime(score.getExamTime());
        this.updateById(db);
    }

    @Override
    public void removeScore(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException(ResultCode.SCORE_NOT_FOUND);
        }
        this.removeById(id);
    }

    @Override
    public ScoreVO getScore(Long id) {
        ScoreVO vo = baseMapper.selectScoreById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.SCORE_NOT_FOUND);
        }
        return vo;
    }

    @Override
    public StudentScoreStatsVO statsByStudent(Long studentId) {
        if (studentMapper.selectById(studentId) == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_FOUND);
        }
        // 聚合 SQL 永远返回一行：没有成绩时 courseCount=0、avgScore=null，前端据此显示"暂无成绩"
        return baseMapper.statsByStudent(studentId);
    }

    @Override
    public CourseScoreStatsVO statsByCourse(Long courseId) {
        if (courseMapper.selectById(courseId) == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }
        return baseMapper.statsByCourse(courseId);
    }
}
