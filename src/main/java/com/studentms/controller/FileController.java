package com.studentms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studentms.annotation.OperationLog;
import com.studentms.common.Result;
import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.entity.SysFile;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件接口
 * <pre>
 * POST /files/upload          上传（需登录），multipart 表单字段名 file
 * GET  /files/view/{filename} 读取（公开：头像要能在 &lt;img src&gt; 里直接加载，
 *                             而 img 标签没法带 Authorization 头）
 * GET  /files/download/{id}   下载（需登录）：按文件ID查元数据，以原始文件名触发下载
 * GET  /files/list            列表（需登录）：分页查询已上传文件
 * </pre>
 * 读取路径在 WebMvcConfig 的拦截器白名单里（/files/view/**）；上传、下载、列表仍走登录校验。
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

    /**
     * 重新下载：按文件ID查元数据（原始文件名 / 磁盘名 / 类型），以原始文件名触发下载。
     * <p>
     * 下载比公开的 view 更敏感（会暴露原始文件名），所以不走白名单——默认需登录。
     * 路径穿越防护复用的是 FileService.resolve 的双闸门，磁盘文件被手动删过也会 404。
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysFile meta = fileService.getMeta(id);
        if (meta == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        Path file = fileService.resolve(meta.getStoredName());
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        String contentType = meta.getContentType() != null ? meta.getContentType() : Files.probeContentType(file);
        response.setContentType(contentType != null ? contentType : "application/octet-stream");
        // RFC 5987：filename*=UTF-8''... 支持中文原始文件名；ASCII 名由 filename="..." 兜底
        String encoded = URLEncoder.encode(meta.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        Files.copy(file, response.getOutputStream());
    }

    /**
     * 文件列表（分页）：GET /files/list?originalName=头像&uploadedBy=2&pageNum=1&pageSize=20
     * <p>
     * uploadedBy 传当前用户ID 即"我的上传"管理页；不传则返回全部（需登录即可访问）。
     */
    @GetMapping("/list")
    public Result<Page<FileVO>> list(@RequestParam(required = false) String originalName,
                                     @RequestParam(required = false) Long uploadedBy,
                                     @RequestParam(required = false) Integer pageNum,
                                     @RequestParam(required = false) Integer pageSize) {
        return Result.success(fileService.pageFiles(originalName, uploadedBy, pageNum, pageSize));
    }
}
