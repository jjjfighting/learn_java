package com.studentms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.studentms.entity.Score;
import com.studentms.vo.CourseScoreStatsVO;
import com.studentms.vo.ScoreVO;
import com.studentms.vo.StudentScoreStatsVO;

import java.util.List;

/**
 * 成绩业务层接口：五张业务表的最后一块拼图
 */
public interface ScoreService extends IService<Score> {

    /**
     * 两维度查成绩列表：studentId / courseId 至少传一个（Controller 层保证）
     */
    List<ScoreVO> listScores(Long studentId, Long courseId);

    /** 录入成绩（联合唯一查重 + 墓碑清理 + 学生/课程存在性校验） */
    void addScore(Score score);

    /** 修改成绩：只允许改分数和考试日期，学生/课程归属不可变 */
    void updateScore(Long id, Score score);

    /** 删除成绩记录 */
    void removeScore(Long id);

    /** 成绩详情（回显学生、课程信息），不存在时抛 SCORE_NOT_FOUND */
    ScoreVO getScore(Long id);

    /** 学生维度统计：科目数 / 平均分 / 最高分 / 总分 */
    StudentScoreStatsVO statsByStudent(Long studentId);

    /** 课程维度统计：参考人数 / 平均分 / 最高分 / 最低分 */
    CourseScoreStatsVO statsByCourse(Long courseId);
}
