package com.studentms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studentms.common.ResultCode;
import com.studentms.common.UserContext;
import com.studentms.common.UserInfo;
import com.studentms.common.exception.BusinessException;
import com.studentms.config.StorageProperties;
import com.studentms.entity.SysFile;
import com.studentms.mapper.SysFileMapper;
import com.studentms.vo.FileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
 * 文件存储服务：上传落盘 + 元数据落库 + 安全读取
 * <p>
 * 安全四件套贯穿其中：① 类型白名单 ② 大小限制（yml 的 multipart 配置兜底）
 * ③ UUID 改名 ④ 路径穿越防护。
 * <p>
 * 落库让"上传即永久"——文件参数持久化到 sys_file 表后，前端拿到文件 ID
 * 随时能重新下载，不再依赖磁盘名拼 URL；原始文件名也保住了。
 */
@Slf4j
@Service
public class FileService {

    /** 存储根目录的绝对规范路径，所有读写都以它为基准 */
    private final Path storageRoot;

    private final Set<String> allowedTypes;

    /** 文件元数据表：落盘后插入一条记录，文件才能被按 ID 重新下载 */
    private final SysFileMapper fileMapper;

    /** 合法存储文件名：UUID（可带扩展名），除此之外的字符一律拒绝——路径穿越攻击在这一步就死了 */
    private static final Pattern STORED_NAME_PATTERN = Pattern.compile("[a-f0-9]{32}\\.(jpg|jpeg|png|gif|webp)");

    public FileService(StorageProperties properties, SysFileMapper fileMapper) {
        this.storageRoot = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        this.allowedTypes = Stream.of(properties.getAllowedTypes().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        this.fileMapper = fileMapper;
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("创建存储目录失败：" + storageRoot, e);
        }
        log.info("文件存储目录：{}", storageRoot);
    }

    /**
     * 保存上传文件：落盘 + 元数据落库，返回访问信息
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
        // ④ 先落盘再落库：库里只会出现"确实落盘成功"的文件，不会冒出有记录无文件的幽灵记录
        try {
            Files.copy(file.getInputStream(), storageRoot.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件写入失败：{}", storedName, e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文件保存失败");
        }
        // ⑤ 元数据落库；插入失败必须回滚磁盘文件，避免留下不可达的孤儿文件
        SysFile meta = new SysFile();
        meta.setOriginalName(original);
        meta.setStoredName(storedName);
        meta.setContentType(contentType);
        meta.setSize(file.getSize());
        UserInfo user = UserContext.get();
        meta.setUploadedBy(user == null ? null : user.getId());
        try {
            fileMapper.insert(meta);
        } catch (Exception e) {
            log.error("文件元数据落库失败，回滚磁盘文件：{}", storedName, e);
            try {
                Files.deleteIfExists(storageRoot.resolve(storedName));
            } catch (IOException ex) {
                log.error("回滚磁盘文件失败：{}", storedName, ex);
            }
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文件保存失败");
        }
        FileVO vo = new FileVO();
        vo.setId(meta.getId());
        vo.setUrl("/files/view/" + storedName);
        vo.setOriginalName(original);
        vo.setStoredName(storedName);
        vo.setSize(file.getSize());
        vo.setContentType(contentType);
        return vo;
    }

    /**
     * 按 ID 查文件元数据；不存在或已逻辑删除返回 null
     */
    public SysFile getMeta(Long id) {
        return fileMapper.selectById(id);
    }

    /**
     * 分页查询文件记录，支持按原始文件名模糊、按上传人筛选
     */
    public Page<FileVO> pageFiles(String originalName, Long uploadedBy, Integer pageNum, Integer pageSize) {
        Page<SysFile> page = fileMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize),
                new LambdaQueryWrapper<SysFile>()
                        .like(StringUtils.hasText(originalName), SysFile::getOriginalName, originalName)
                        .eq(uploadedBy != null, SysFile::getUploadedBy, uploadedBy)
                        .orderByDesc(SysFile::getId));
        // Page.convert() 返回的是 IPage<R> 而非 Page<R>，这里手动转回 Page<FileVO>，保持返回类型与声明一致
        Page<FileVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    /** 实体转 VO：url 由磁盘名拼出，供前端展示/重新下载 */
    private FileVO toVO(SysFile meta) {
        FileVO vo = new FileVO();
        vo.setId(meta.getId());
        vo.setUrl("/files/view/" + meta.getStoredName());
        vo.setOriginalName(meta.getOriginalName());
        vo.setStoredName(meta.getStoredName());
        vo.setSize(meta.getSize());
        vo.setContentType(meta.getContentType());
        return vo;
    }

    /**
     * 按文件名解析出磁盘路径；文件名非法、越界或不存在时返回 null
     * <p>
     * 路径穿越防护两道闸：格式白名单（只认 32 位十六进制名）+ normalize 后的 startsWith 复检，
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
