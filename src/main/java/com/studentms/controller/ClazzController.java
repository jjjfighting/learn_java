package com.studentms.controller;

import com.studentms.common.Result;
import com.studentms.entity.Clazz;
import com.studentms.service.ClazzService;
import com.studentms.vo.ClazzVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 班级模块接口（与学生模块同构的 RESTful 模板）
 * <pre>
 * GET    /clazzes          列表（含在校学生数）
 * GET    /clazzes/{id}     详情
 * POST   /clazzes          新增
 * PUT    /clazzes/{id}     修改
 * DELETE /clazzes/{id}     删除（班里有学生时返回 3002）
 * </pre>
 * URL 沿用 clazz 命名：class 是 Java 关键字，全链路统一用 clazz 避免混淆。
 */
@RestController
@RequestMapping("/clazzes")
@RequiredArgsConstructor
public class ClazzController {

    private final ClazzService clazzService;

    /** 班级列表：数据量小（全校几十上百个班），不做分页，直接全量 + 人数统计 */
    @GetMapping
    public Result<List<ClazzVO>> list() {
        return Result.success(clazzService.listClazzes());
    }

    /** 班级详情 */
    @GetMapping("/{id}")
    public Result<Clazz> get(@PathVariable Long id) {
        return Result.success(clazzService.getClazz(id));
    }

    /** 新增班级 */
    @PostMapping
    public Result<Long> add(@Valid @RequestBody Clazz clazz) {
        clazzService.addClazz(clazz);
        return Result.success("新增成功", clazz.getId());
    }

    /** 修改班级 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Clazz clazz) {
        clazzService.updateClazz(id, clazz);
        return Result.success();
    }

    /** 删除班级（逻辑删除；班里有学生会被 Service 拦下，返回 3002） */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        clazzService.removeClazz(id);
        return Result.success();
    }
}
