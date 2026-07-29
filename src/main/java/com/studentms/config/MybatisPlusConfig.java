package com.studentms.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * <p>
 * @MapperScan：一次性把 com.studentms.mapper 包下所有 Mapper 接口扫描进 Spring 容器，
 * 之后每个模块的 XxxMapper 都不用再单独加 @Mapper 注解。
 */
@Configuration
@MapperScan("com.studentms.mapper")
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器，并加入"分页"内部拦截器
     * <p>
     * 注意：如果不注册这个 Bean，后面给 selectPage() 传 Page 对象也不会生效，
     * 查询会返回全表数据，只是 Page 里的分页数字不对——这是初学者最常踩的坑。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页内部拦截器，指定数据库方言为 MySQL（它才知道该拼 LIMIT 语句）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
