package com.studentms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.studentms.entity.OpLog;

/**
 * 操作日志业务层：只有查询，日志"只写不改"——不提供增删改接口
 */
public interface OpLogService extends IService<OpLog> {

    /** 分页查询日志（module / username 可选过滤） */
    Page<OpLog> pageLogs(String module, String username, Integer pageNum, Integer pageSize);
}
