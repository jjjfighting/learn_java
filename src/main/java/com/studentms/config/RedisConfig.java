package com.studentms.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置
 * <p>
 * @EnableCaching：开启 Spring Cache 抽象——从此 @Cacheable / @CacheEvict 注解才生效。
 * 默认缓存管理器是 ConcurrentMapCacheManager（进程内内存），这里换成 RedisCacheManager，
 * 缓存真正落到 Redis，多实例共享、重启不丢。
 * <p>
 * 两个教学重点：
 * 1. 值用 GenericJackson2JsonRedisSerializer 存成可读 JSON，而不是 JDK 二进制序列化——
 *    好处：Redis 里能直接看、体积小、换语言/工具也能读；
 * 2. 序列化器里的 ObjectMapper 必须注册 JavaTimeModule——
 *    项目里的 LocalDate（考试日期）、LocalDateTime（createTime）默认序列化会抛
 *    InvalidDefinitionException，这是 Redis 缓存 + JDK8 时间类型最常见的坑。
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ---- 值序列化器：JSON + 类型信息 ----
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());                    // LocalDate/LocalDateTime 支持
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 日期存成 "2026-01-10" 而非时间戳数组
        // 存类型信息：反序列化时才能把 JSON 还原成原来的 VO/实体类，而不是 LinkedHashMap
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        // ---- 默认缓存配置：TTL 10 分钟，值走上面的 JSON 序列化 ----
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));

        // ---- 按缓存名覆盖 TTL：成绩统计变化勤，TTL 短一点 ----
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("courseList", defaultConfig);
        cacheConfigs.put("clazzList", defaultConfig);
        cacheConfigs.put("scoreStatsByStudent", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("scoreStatsByCourse", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
