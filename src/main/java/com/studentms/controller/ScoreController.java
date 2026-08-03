package com.studentms.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.studentms.annotation.OperationLog;
import com.studentms.annotation.RequireRole;
import com.studentms.common.Result;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.entity.Course;
import com.studentms.entity.Score;
import com.studentms.service.CourseService;
import com.studentms.service.ScoreService;
import com.studentms.vo.CourseScoreStatsVO;
import com.studentms.vo.ScoreExportRow;
import com.studentms.vo.ScoreVO;
import com.studentms.vo.StudentScoreStatsVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
 * GET    /scores/export?courseId=1              导出课程成绩单 Excel（仅 TEACHER / ADMIN）
 * </pre>
 */
@RestController
@RequestMapping("/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    /** 导出成绩单时需要课程存在性校验和课程名（文件名用），只注入 Mapper 的上一级 Service */
    private final CourseService courseService;

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
    @OperationLog(module = "score", action = "CREATE")
    @RequireRole({"TEACHER", "ADMIN"})
    @PostMapping
    public Result<Long> add(@Valid @RequestBody Score score) {
        scoreService.addScore(score);
        return Result.success("录入成功", score.getId());
    }

    @OperationLog(module = "score", action = "UPDATE")
    @RequireRole({"TEACHER", "ADMIN"})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Score score) {
        scoreService.updateScore(id, score);
        return Result.success();
    }

    @OperationLog(module = "score", action = "DELETE")
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

    /**
     * 导出课程成绩单 Excel（带样式表头），仅 TEACHER / ADMIN 可下载
     * <p>
     * 成绩单暴露该课程所有学生的成绩，比单条查询敏感，所以不放行公开白名单。
     * 数据直接复用 listScores(courseId) 的 JOIN 查询，转成 ScoreExportRow 后由 EasyExcel 写出。
     */
    @OperationLog(module = "score", action = "EXPORT")
    @RequireRole({"TEACHER", "ADMIN"})
    @GetMapping("/export")
    public void export(@RequestParam Long courseId, HttpServletResponse response) throws IOException {
        // 课程不存在直接 404，避免"导出一个空成绩单"让用户以为数据丢了
        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }
        List<ScoreVO> scores = scoreService.listScores(null, courseId);
        // 响应头：Excel 的 MIME 类型 + 中文文件名（RFC 5987，参照文件下载接口的写法）
        List<ScoreExportRow> rows = scores.stream().map(ScoreExportRow::from).toList();
        String fileName = URLEncoder.encode("成绩单_" + course.getCourseName() + ".xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        // 表头样式：灰底 + 加粗 + 居中 + 细边框；数据行样式留空用默认
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headStyle.setBorderLeft(BorderStyle.THIN);
        headStyle.setBorderRight(BorderStyle.THIN);
        headStyle.setBorderTop(BorderStyle.THIN);
        headStyle.setBorderBottom(BorderStyle.THIN);
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headStyle.setWriteFont(headFont);
        HorizontalCellStyleStrategy strategy = new HorizontalCellStyleStrategy(headStyle, new WriteCellStyle());
        // EasyExcel 流式写出：写进 response 输出流，ExcelWriter 是 AutoCloseable，自动关流
        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream(), ScoreExportRow.class)
                .registerWriteHandler(strategy)
                .build()) {
            WriteSheet sheet = EasyExcel.writerSheet("成绩单").build();
            writer.write(rows, sheet);
        }
    }
}
