# 学生管理系统 · 第十二步：Redis 缓存热门查询

> 本篇完成进阶五件套收尾的第一半：把热门查询（课程列表、班级列表、成绩统计）缓存进 Redis。用 **Spring Cache 抽象 + 注解声明式**搞定——业务方法只多一行注解，命中 / 写入 / 失效全部交给框架，代码零侵入。全部用例需在你本地联调确认（检查单见第六节）。

## 一、为什么缓存这些查询

"热门查询" = **读多写少** + **查询成本高** 两类，本步各取代表：

| 接口 | 为什么值得缓存 |
|---|---|
| `GET /courses` | 读多写少、近乎静态；且是 LEFT JOIN 教师表的多表查询 |
| `GET /clazzes` | 同上；还是 LEFT JOIN 学生表的分组统计 SQL |
| `GET /scores/stats/by-student` | 聚合 SQL（COUNT/AVG/MAX/SUM），数据量大了每次重算很贵 |
| `GET /scores/stats/by-course` | 同上 |

学生分页这种带条件、带分页的查询不是本次目标（缓存 key 会膨胀、命中率低），留作扩展话题。

## 二、为什么是 Spring Cache + Redis，而不是手写

| 方案 | 优劣 |
|---|---|
| **Spring Cache 注解 + Redis（本步采用）** | `@Cacheable` / `@CacheEvict` 声明式挂载，命中判断、写缓存、删缓存、key 生成全由框架包办；换缓存实现（本地 → Redis）只改配置不碰业务 |
| 手动 `RedisTemplate` | 每次查询都要自己判命中、写回、删 key、设计 key，样板代码淹没业务 |
| MyBatis-Plus 二级缓存 | 侵入 Mapper XML 和 Cache 类，失效粒度粗 |

**Spring Cache 的三个注解语义**（本步用到前两个）：

- `@Cacheable`：方法被调用时先查缓存，**命中直接返回、不执行方法**；没命中才执行并把返回值写进缓存；
- `@CacheEvict`：方法**执行成功后退掉缓存**（默认 `beforeInvocation=false`，方法抛异常就不会误清缓存）；
- `@CachePut`：方法**执行后总是更新缓存**（本步未用，写读同源场景才需要）。

## 三、依赖与启动 Redis

### 依赖（`pom.xml`）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 启动 Redis（Docker，本机无原生 Redis）

```bash
docker run -d --name studentms-redis -p 6379:6379 redis:7-alpine
```

### 连接配置（`application.yml`）

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3s      # 连接超时：Redis 挂了快速失败，而不是挂起请求
```

**Redis 必须运行**：缓存命中/写入是同步的，Redis 连不上时缓存查询会抛连接异常转 500（生产环境应加 `CacheErrorHandler` 兜底降级，教学阶段先记录这个账）。

## 四、配置：RedisConfig

`config/RedisConfig.java` 是本步的核心，三个教学点：

### 1. `@EnableCaching` —— 注解生效的开关

不加它，所有 `@Cacheable` 都只是普通注解，静默失效。`@EnableCaching` + 一个 `RedisCacheManager` Bean，Spring 就把默认的进程内缓存换成 Redis。

### 2. 值序列化：JSON + 类型信息

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());                    // ① LocalDate/LocalDateTime 支持
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ② 日期存成 "2026-01-10" 而非时间戳数组
mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(),
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY);                              // ③ 存类型信息
GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(mapper);
```

- **①③ 必须成套**：不注册 `JavaTimeModule`，缓存 `LocalDate/LocalDateTime`（考试日期、createTime）会抛 `InvalidDefinitionException`——这是 Redis 缓存 + JDK8 时间类型最常见的坑；不存类型信息，反序列化会把 VO 还原成 `LinkedHashMap`，类型全错。
- 结果：Redis 里存的是**可读 JSON**，`redis-cli` 直接能看，比如 `scoreStatsByStudent::1` 的值是 `{"@class":"...StudentScoreStatsVO","studentId":1,...}`。

### 3. 分缓存 TTL

```java
RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
// 成绩统计变化勤，单独覆盖成 5 分钟
cacheConfigs.put("scoreStatsByStudent", defaultConfig.entryTtl(Duration.ofMinutes(5)));
```

列表缓存 10 分钟、统计缓存 5 分钟——同一个 `RedisCacheManager` 里按缓存名给不同 TTL。

## 五、注解挂载：四个 Service

### 缓存清单

| 缓存名 | 挂载点（Service 实现） | key | TTL |
|---|---|---|---|
| `courseList` | `CourseServiceImpl.listCourses` | `'all'` | 10 分钟 |
| `clazzList` | `ClazzServiceImpl.listClazzes` | `'all' | 10 分钟 |
| `scoreStatsByStudent` | `ScoreServiceImpl.statsByStudent` | `#studentId` | 5 分钟 |
| `scoreStatsByCourse` | `ScoreServiceImpl.statsByCourse` | `#courseId` | 5 分钟 |

- 无参列表用固定 key `'all'`（SpEL 单引号表示字符串字面量）；
- 统计用 SpEL 引用方法参数 `#studentId` / `#courseId`——每个学生/课程各占一个 key，互不干扰。

### 失效矩阵（注解的另一半）

| 写操作 | 失效缓存 | 原因 |
|---|---|---|
| Course 增 / 删 | `courseList` | 列表变了 |
| Course 改 | `courseList` + `scoreStatsByCourse` | 课程改名影响统计回显的 courseName |
| Clazz 增 / 改 / 删 | `clazzList` | 列表变了 |
| **Student 增 / 改 / 删 / Excel导入** | **`clazzList`** | **班级列表含"在校学生数"，学生一动人数就变** |
| Score 增 / 改 / 删 | `scoreStatsByStudent` + `scoreStatsByCourse` | 统计要重算 |

```java
// 失效写法：一次清空整个缓存（allEntries=true），不用精确到单个 key
@CacheEvict(cacheNames = {"scoreStatsByStudent", "scoreStatsByCourse"}, allEntries = true)
```

**关键洞察**：失效矩阵本质是**数据依赖关系的倒影**——`clazzList` 虽然由班级表驱动，但它 JOIN 了学生表算人数，所以学生侧的写操作也必须失效它。这类"跨表依赖"是缓存失效设计最容易漏的地方。

## 六、改动清单

```
pom.xml                          + spring-boot-starter-data-redis
application.yml                  + spring.data.redis 配置
config/RedisConfig.java          新增：@EnableCaching + RedisCacheManager + JSON 序列化 + 分缓存 TTL
service/impl/CourseServiceImpl   @Cacheable / @CacheEvict
service/impl/ClazzServiceImpl    @Cacheable / @CacheEvict
service/impl/ScoreServiceImpl    @Cacheable / @CacheEvict（统计缓存 + 成绩写失效）
service/impl/StudentServiceImpl  写操作 + @CacheEvict(clazzList)（含 Excel 导入）
```

## 七、联调检查单（在你的环境实测）

> 预期结果如下，起 Redis + 应用后逐条验证。

| # | 用例 | 预期结果 |
|---|---|---|
| R1 | `docker ps` 看到 studentms-redis | 容器运行中，`redis-cli ping` 返回 PONG |
| R2 | 首次 `GET /courses` | 控制台打印 `SELECT ... FROM course ... LEFT JOIN sys_user`（走 DB） |
| R3 | 再次 `GET /courses` | 控制台**不再打印查询 SQL**（命中缓存），返回一致 |
| R4 | `redis-cli keys '*'` | 出现 `courseList::all`、`clazzList::all`；`GET courseList::all` 是可读 JSON |
| R5 | `GET /scores/stats/by-student?studentId=1` | 出现 `scoreStatsByStudent::1`，且 `ttl` 约 300 秒（5 分钟） |
| R6 | 录入一条新成绩后立刻再查统计 | 统计缓存被清空、重新查库，数值包含新成绩 |
| R7 | 新增一个学生后立刻查 `GET /clazzes` | 班级人数立即更新（clazzList 已被失效） |
| R8 | 修改课程名后查 `GET /scores/stats/by-course` | courseName 是改名后的新值 |
| R9 | 停掉 Redis 再调 `GET /courses` | 抛 Redis 连接异常（证明 Redis 是前置依赖，生产需加降级） |

## 八、高频坑位

1. **忘了 `@EnableCaching`**：注解全部"静默失效"，查了 Redis 却没有 key——最隐蔽的坑，先查这个。
2. **`LocalDate/LocalDateTime` 序列化报错**：默认 `ObjectMapper` 没有 `JavaTimeModule`，缓存这类值直接抛异常。必须像 RedisConfig 那样手工配。
3. **不存类型信息反序列化变成 `LinkedHashMap`**：`GenericJackson2JsonRedisSerializer` 需要 `activateDefaultTyping`，否则前端拿到的是 map 结构、类型判断全错。
4. **缓存打在 Controller 还是 Service**：注解要放在**被 Spring 代理的方法**上——Controller 调 Service 走代理，所以打在 Service 实现方法；同 Service 内部自调用会绕过代理、缓存失效。
5. **失效矩阵漏掉跨表依赖**：`clazzList` 依赖学生表，学生增删改（含 Excel 导入）必须失效它——只盯着"本表写操作"设计失效，是最常见的错误。
6. **Redis 挂 = 接口 500**：缓存操作是同步的。教学先接受，生产用 `CacheErrorHandler` 捕获缓存异常、降级直查 DB。

## 九、下一步预告

**第十三步**：Spring `@Scheduled` 定时任务收尾——届时把第十步遗留的"清理孤儿文件"（磁盘上有、`sys_file` 里没有的记录）用定时任务自动清扫，进阶五件套正式收官。
