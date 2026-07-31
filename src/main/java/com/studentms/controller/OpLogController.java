package com.studentms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studentms.annotation.RequireRole;
import com.studentms.common.Result;
import com.studentms.entity.OpLog;
import com.studentms.service.OpLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询接口：仅管理员可见
 * <pre>
 * GET /logs?module=student&username=admin&pageNum=1&pageSize=20
 * </pre>
 * 审计数据敏感，角色门禁直接交给拦截器的 @RequireRole——教师/学生角色访问返回 403。
 */
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class OpLogController {

    private final OpLogService opLogService;

    @RequireRole("ADMIN")
    @GetMapping
    public Result<Page<OpLog>> page(@RequestParam(required = false) String module,
                                    @RequestParam(required = false) String username,
                                    @RequestParam(required = false) Integer pageNum,
                                    @RequestParam(required = false) Integer pageSize) {
        return Result.success(opLogService.pageLogs(module, username, pageNum, pageSize));
    }
}
