package com.studentms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studentms.entity.SysFile;

/**
 * 文件数据访问层：上传时插入元数据、按 ID 查记录、分页查询，BaseMapper 足够
 */
public interface SysFileMapper extends BaseMapper<SysFile> {
}
