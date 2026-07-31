package com.studentms.service;

import com.studentms.common.ResultCode;
import com.studentms.common.exception.BusinessException;
import com.studentms.config.StorageProperties;
import com.studentms.vo.FileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件存储服务：上传落盘 + 安全读取
 * <p>
 * 安全四件套贯穿其中：① 类型白名单 ② 大小限制（yml 的 multipart 配置兜底）
 * ③ UUID 改名 ④ 路径穿越防护。
 */
@Slf4j
@Service
public class FileService {

    /** 存储根目录的绝对规范路径，所有读写都以它为基准 */
    private final Path storageRoot;

    private final Set<String> allowedTypes;

    /** 合法存储文件名：UUID（可带扩展名），除此之外的字符一律拒绝——路径穿越攻击在这一步就死了 */
    private static final Pattern STORED_NAME_PATTERN = Pattern.compile("[a-f0-9]{32}\\.(jpg|jpeg|png|gif|webp)");

    public FileService(StorageProperties properties) {
        this.storageRoot = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        this.allowedTypes = Stream.of(properties.getAllowedTypes().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("创建存储目录失败：" + storageRoot, e);
        }
        log.info("文件存储目录：{}", storageRoot);
    }

    /**
     * 保存上传文件，返回访问信息
     */
    public FileVO store(MultipartFile file) {
        // ① 空文件拦截
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);
        }
        // ② 类型白名单：认 Content-Type（浏览器按文件内容探测），不认扩展名——
        //    扩展名可以随便改，骗不过 MIME 检查 + 后续的图片渲染验证
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        // ③ UUID 改名：原始文件名可能带 ../、空格、中文、重名——统一丢弃，只保留扩展名
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.lastIndexOf('.') >= 0) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.copy(file.getInputStream(), storageRoot.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件写入失败：{}", storedName, e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文件保存失败");
        }
        FileVO vo = new FileVO();
        vo.setUrl("/files/view/" + storedName);
        vo.setOriginalName(original);
        vo.setStoredName(storedName);
        vo.setSize(file.getSize());
        vo.setContentType(contentType);
        return vo;
    }

    /**
     * 按文件名解析出磁盘路径；文件名非法、越界或不存在时返回 null
     * <p>
     * ④ 路径穿越防护两道闸：格式白名单（只认 32 位十六进制名）+ normalize 后的 startsWith 复检，
     * 双保险杜绝 ../../application.yml 这类读取越权。
     */
    public Path resolve(String filename) {
        if (filename == null || !STORED_NAME_PATTERN.matcher(filename).matches()) {
            return null;
        }
        Path target = storageRoot.resolve(filename).normalize();
        if (!target.startsWith(storageRoot) || !Files.exists(target)) {
            return null;
        }
        return target;
    }
}
