package com.studentms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studentms.entity.OpLog;

/**
 * 操作日志数据访问层：只需要插入和分页查询，BaseMapper 足够
 */
public interface OpLogMapper extends BaseMapper<OpLog> {
}
