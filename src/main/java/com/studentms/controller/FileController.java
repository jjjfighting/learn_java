package com.studentms.controller;

import com.studentms.annotation.OperationLog;
import com.studentms.common.Result;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.service.FileService;
import com.studentms.vo.FileVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件接口
 * <pre>
 * POST /files/upload          上传（需登录），multipart 表单字段名 file
 * GET  /files/view/{filename} 读取（公开：头像要能在 &lt;img src&gt; 里直接加载，
 *                             而 img 标签没法带 Authorization 头）
 * </pre>
 * 读取路径在 WebMvcConfig 的拦截器白名单里；上传仍走登录校验。
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /** 上传文件，表单字段名必须是 file（前端 FormData.append('file', ...)） */
    @OperationLog(module = "file", action = "UPLOAD")
    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", fileService.store(file));
    }

    /**
     * 文件读取：直接把字节流写进响应。
     * {filename:.+} 的正则限定是为了让带点的文件名不被 Spring 的路径匹配截断。
     */
    @GetMapping("/view/{filename:.+}")
    public void view(@PathVariable String filename, HttpServletResponse response) throws IOException {
        Path file = fileService.resolve(filename);
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        String contentType = Files.probeContentType(file);
        response.setContentType(contentType != null ? contentType : "application/octet-stream");
        // 浏览器内联展示（图片直接显示）；想触发下载就改成 attachment
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        Files.copy(file, response.getOutputStream());
    }
}
