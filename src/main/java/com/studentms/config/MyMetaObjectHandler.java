package com.studentms.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 公共字段自动填充处理器
 * <p>
 * 作用：新增数据时自动填 createTime / updateTime，更新数据时自动填 updateTime，
 * 业务代码不用再手写 entity.setCreateTime(LocalDateTime.now())。
 * <p>
 * 生效前提：实体类字段上要标注填充时机，如：
 * <pre>
 * &#64;TableField(fill = FieldFill.INSERT)         // 新增时填充
 * private LocalDateTime createTime;
 *
 * &#64;TableField(fill = FieldFill.INSERT_UPDATE)  // 新增和更新时都填充
 * private LocalDateTime updateTime;
 * </pre>
 * 自动填充认的是"实体属性名"（createTime），不是数据库列名（create_time），
 * 列名的映射由 yml 里的 map-underscore-to-camel-case 负责，两者各司其职。
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /** 执行 insert() / save() 等新增方法时触发 */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // strictInsertFill：仅当实体字段为 null 时才填充，业务代码手动设过的值不会被覆盖
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    /** 执行 updateById() / update() 等更新方法时触发 */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
