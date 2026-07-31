package com.studentms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置：绑定 yml 里 storage.* 的配置项
 * <p>
 * @ConfigurationProperties 是 SpringBoot 的类型安全配置绑定——
 * 比一堆 @Value 强的地方：集中、可校验、IDE 能自动补全配置键。
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** 文件落盘目录 */
    private String location;

    /** 允许的文件类型白名单（逗号分隔的 Content-Type） */
    private String allowedTypes;
}
