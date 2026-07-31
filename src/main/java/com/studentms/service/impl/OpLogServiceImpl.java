package com.studentms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studentms.entity.OpLog;
import com.studentms.mapper.OpLogMapper;
import com.studentms.service.OpLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志业务层实现
 */
@Service
public class OpLogServiceImpl extends ServiceImpl<OpLogMapper, OpLog> implements OpLogService {

    @Override
    public Page<OpLog> pageLogs(String module, String username, Integer pageNum, Integer pageSize) {
        return this.lambdaQuery()
                .eq(StringUtils.hasText(module), OpLog::getModule, module)
                .like(StringUtils.hasText(username), OpLog::getUsername, username)
                .orderByDesc(OpLog::getId)
                .page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize));
    }
}
