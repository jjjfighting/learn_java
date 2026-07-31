package com.studentms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studentms.entity.SysUser;

/**
 * 系统用户数据访问层
 * <p>
 * 本步供课程模块查教师；认证模块（第七步）的登录校验也复用它。
 */
public interface UserMapper extends BaseMapper<SysUser> {
}
