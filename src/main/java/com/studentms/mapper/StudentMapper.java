package com.studentms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studentms.entity.Student;

/**
 * 学生数据访问层（DAO / Mapper）
 * <p>
 * 继承 BaseMapper<Student> 就免费获得一整套单表 CRUD：
 * insert / deleteById / updateById / selectById / selectList / selectPage ...
 * 一行 SQL 都不用写。简单增删改查用它就够，复杂 SQL 以后再写 XML 或 @Select 扩展。
 * <p>
 * 注意这是个接口，没有实现类——运行时由 MyBatis 用"动态代理"凭空生成实现对象，
 * 再通过 MybatisPlusConfig 上的 @MapperScan 注册进 Spring 容器。
 */
public interface StudentMapper extends BaseMapper<Student> {
}
