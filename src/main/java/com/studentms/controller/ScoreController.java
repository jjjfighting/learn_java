package com.studentms.controller;

import com.studentms.annotation.RequireRole;
import com.studentms.common.Result;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.entity.Score;
import com.studentms.service.ScoreService;
import com.studentms.vo.CourseScoreStatsVO;
import com.studentms.vo.ScoreVO;
import com.studentms.vo.StudentScoreStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 成绩模块接口（五张业务表的最后一块）
 * <pre>
 * GET    /scores?studentId=1            按学生查成绩单
 * GET    /scores?courseId=1             按课程查成绩表
 * GET    /scores/{id}                   成绩详情
 * POST   /scores                        录入（仅 TEACHER / ADMIN）
 * PUT    /scores/{id}                   改分（仅 TEACHER / ADMIN）
 * DELETE /scores/{id}                   删除（仅 TEACHER / ADMIN）
 * GET    /scores/stats/by-student?studentId=1   学生维度统计
 * GET    /scores/stats/by-course?courseId=1     课程维度统计
 * </pre>
 */
@RestController
@RequestMapping("/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    /** 两维度列表：studentId / courseId 至少传一个，都不传直接 400——防止误查全表 */
    @GetMapping
    public Result<List<ScoreVO>> list(@RequestParam(required = false) Long studentId,
                                      @RequestParam(required = false) Long courseId) {
        if (studentId == null && courseId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请至少指定一个查询维度：studentId 或 courseId");
        }
        return Result.success(scoreService.listScores(studentId, courseId));
    }

    @GetMapping("/{id}")
    public Result<ScoreVO> get(@PathVariable Long id) {
        return Result.success(scoreService.getScore(id));
    }

    /** @RequireRole 支持多角色：教师和管理员都能录入，其他角色 403 */
    @RequireRole({"TEACHER", "ADMIN"})
    @PostMapping
    public Result<Long> add(@Valid @RequestBody Score score) {
        scoreService.addScore(score);
        return Result.success("录入成功", score.getId());
    }

    @RequireRole({"TEACHER", "ADMIN"})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Score score) {
        scoreService.updateScore(id, score);
        return Result.success();
    }

    @RequireRole({"TEACHER", "ADMIN"})
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        scoreService.removeScore(id);
        return Result.success();
    }

    /** 学生维度统计：科目数 / 平均分 / 最高分 / 总分 */
    @GetMapping("/stats/by-student")
    public Result<StudentScoreStatsVO> statsByStudent(@RequestParam Long studentId) {
        return Result.success(scoreService.statsByStudent(studentId));
    }

    /** 课程维度统计：参考人数 / 平均分 / 最高分 / 最低分 */
    @GetMapping("/stats/by-course")
    public Result<CourseScoreStatsVO> statsByCourse(@RequestParam Long courseId) {
        return Result.success(scoreService.statsByCourse(courseId));
    }
}
