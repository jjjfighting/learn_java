# 学生管理系统 · 第五步：班级模块 CRUD 与关联处理

> 本篇完成：用第四步成型的模板搭建班级模块（实体 / Mapper / Service / Controller 全套），重点攻克三处表间关联：① 班级列表带在校学生人数统计（LEFT JOIN 自定义 SQL）；② 删除班级与外键约束 `fk_student_clazz (RESTRICT)` 的协作（业务预检 + 数据库兜底）；③ 学生详情回显班级名称（VO + 跨模块查询）。15 个联调用例实测通过。

## 一、模板复用：第二个模块快在哪里

班级模块和学生模块的文件结构完全同构：

| 学生模块（第四步） | 班级模块（本步） | 差异 |
|---|---|---|
| `entity/Student.java` | `entity/Clazz.java` | 字段不同，注解套路相同 |
| `mapper/StudentMapper.java` | `mapper/ClazzMapper.java` | **多了 `@Select` 自定义 SQL** |
| `dto/StudentQueryDTO.java` | （无） | 班级数量少，列表不做分页条件查询 |
| `service/StudentService(+Impl)` | `service/ClazzService(+Impl)` | **多了跨模块的学生计数** |
| `controller/StudentController.java` | `controller/ClazzController.java` | 同构 RESTful |
| （无） | `vo/ClazzVO.java`、`vo/StudentVO.java` | **本步新增 VO 层** |

模板成型后，纯 CRUD 部分几乎是肌肉记忆，精力全部花在"和学生模块不一样的地方"——也就是本步的三个关联处理。

> 表名 `clazz` 的由来：`class` 是 Java 关键字不能当类名，索性表名、类名、URL（`/clazzes`）全链路统一用 `clazz`，省得记两套名字。

## 二、关联处理 ①：班级列表带学生人数

### 需求与方案

列表里每个班级要显示"在校学生数"。两种写法：

- **N+1 查询**：查出 N 个班级，再循环 N 次 `COUNT`——班级一多就是性能灾难，Pass；
- **一条 SQL 搞定**：`LEFT JOIN student` 分组统计，一次往返拿全。

### 自定义 SQL（ClazzMapper）

```java
@Select("""
        SELECT c.id, c.clazz_name, c.grade, c.head_teacher, c.description,
               c.create_time, c.update_time,
               COUNT(s.id) AS student_count
        FROM clazz c
        LEFT JOIN student s ON s.clazz_id = c.id AND s.deleted = 0
        WHERE c.deleted = 0
        GROUP BY c.id
        ORDER BY c.id DESC
        """)
List<ClazzVO> selectClazzListWithStudentCount();
```

三个要点，个个都是坑：

**1. `LEFT JOIN` 不能写成 `INNER JOIN`。** 内连接会把"还没有学生的新班级"直接过滤掉——刚建的班在列表里凭空消失。左连接保证所有班级都在，没匹配到学生时 `COUNT(s.id)` 为 0。

**2. 逻辑删除条件必须手写（本步第一大坑）。** MyBatis-Plus 自动拼接的 `AND deleted=0` **只对框架自己生成的 SQL 生效**，`@Select` 自定义 SQL 它管不着。两张表的 `deleted=0` 都要自己写，而且学生的条件必须写在 `ON` 后面，不能写在 `WHERE` 里：

```sql
-- ✅ 正确：过滤条件在 ON 里，LEFT JOIN 语义完整
LEFT JOIN student s ON s.clazz_id = c.id AND s.deleted = 0

-- ❌ 错误：写在 WHERE 里，"没有在校学生的班级"整行被 WHERE 筛掉，左连接退化成内连接
LEFT JOIN student s ON s.clazz_id = c.id
WHERE s.deleted = 0
```

**3. `GROUP BY c.id` 一句就够。** MySQL8 默认开启 `ONLY_FULL_GROUP_BY`，按别的数据库的"常识"似乎要把 SELECT 的每一列都塞进 GROUP BY；但 MySQL 认**函数依赖**——`id` 是 `clazz` 表的主键，分组后该表其他列的值都被唯一确定，允许直接 SELECT。

统计结果映射到 `ClazzVO`（`extends Clazz` + `studentCount` 字段），别名 `student_count` → 驼峰 `studentCount` 由 yml 的 `map-underscore-to-camel-case` 自动完成。VO（View Object）是展示层专用模型：统计字段不进实体类，数据库结构和页面结构彻底解耦。

实测响应：

```json
{"code":200,"message":"操作成功","data":[
  {"id":1,"clazzName":"2023级软件1班","grade":"2023","headTeacher":"王老师",
   "createTime":"...","updateTime":"...","studentCount":2}
]}
```

## 三、关联处理 ②：删除班级与外键约束的协作

### 数据库层的原始设计

建表脚本里学生表有这样一条外键：

```sql
CONSTRAINT fk_student_clazz
    FOREIGN KEY (clazz_id) REFERENCES clazz (id)   -- 默认 RESTRICT：班里还有学生时禁止删班
```

意思是：只要还有学生的 `clazz_id` 指向某个班，数据库就**拒绝删除**这个班。

### 但逻辑删除改变了游戏规则

我们的删除全是逻辑删除（`UPDATE ... SET deleted=1`），而外键只认识真正的 `DELETE` 语句。于是形成了一张有意思的关系矩阵：

| 场景 | 实际发生的 SQL | 外键 RESTRICT 是否触发 | 谁来保护 |
|---|---|---|---|
| 通过 API 删有学生的班级 | `UPDATE clazz SET deleted=1` | ❌ 不触发（这不是 DELETE） | **Service 业务预检 → 3002** |
| 通过 API 删空班级 | `UPDATE clazz SET deleted=1` | — | 正常放行 |
| 手工在数据库执行 `DELETE FROM clazz` | 真 DELETE | ✅ 触发，报错 1451 | 外键兜底 |
| 预检后并发塞进新学生，再物理删 | 真 DELETE | ✅ 触发 | 外键兜底 |

结论：**全逻辑删除的设计下，外键在常规 API 流量里根本不会触发，真正保护数据的是业务层预检**。那外键是不是白设计了？不是——它是针对"绕过应用的直接库操作"和"未来可能出现的物理删除"的最后防线，成本为零，留着。

### 业务预检的实现

```java
// ClazzServiceImpl#removeClazz
Long count = studentMapper.selectCount(
        new LambdaQueryWrapper<Student>().eq(Student::getClazzId, id));
if (count > 0) {
    throw new BusinessException(ResultCode.CLAZZ_HAS_STUDENTS,
            "该班级下还有 " + count + " 名学生，请先转移学生后再删除");
}
this.removeById(id);
```

两个细节：

1. `selectCount` 是框架生成的 SQL，**自动带 `AND deleted=0`**（与自定义 SQL 正相反）——已逻辑删除的学生不占编制，不会误拦；
2. 提示语拼上具体人数，前端可以直接弹"该班级下还有 2 名学生..."，比外键报错那句 `Cannot delete or update a parent row: a foreign key constraint fails` 友好一万倍。

就算预检被并发穿透，万一哪天真走了物理删除，外键抛出的 `DataIntegrityViolationException` 也会被第四步加的全局处理器接住转成 400——双保险闭环。

## 四、关联处理 ③：学生详情回显班级名称

前端看学生详情想要"2023级软件1班"，不是一个冷冰冰的 `clazzId: 1`。改造学生模块三处：

**1. 新增 `StudentVO`**（`extends Student` + `clazzName`），`getStudent` 返回类型改为 `StudentVO`；

**2. 实体拷 VO + 补关联名称**：

```java
StudentVO vo = new StudentVO();
BeanUtils.copyProperties(student, vo);          // 同名字段一把梭（参数顺序：源, 目标）
if (student.getClazzId() != null) {
    Clazz clazz = clazzMapper.selectById(student.getClazzId());
    if (clazz != null) {
        vo.setClazzName(clazz.getClazzName());
    }
}
return vo;
```

**优雅降级**天然成立：学生未分班（`clazzId` 为空）或班级已被逻辑删除（`selectById` 自带 `deleted=0` 过滤查不到），`clazzName` 就是 `null`，详情接口照常返回，不抛错。

**3. 依赖注入只碰对方的 Mapper，不碰对方的 Service。** 这是本步第二个关键设计：

```
ClazzServiceImpl   ──注入──>  StudentMapper      （统计学生数）
StudentServiceImpl ──注入──>  ClazzMapper        （回显班级名）
```

两个 Service 互相需要对方的能力，如果 `ClazzServiceImpl` 注入 `StudentService`、`StudentServiceImpl` 又注入 `ClazzService`，就构成 **Service 循环依赖**（构造器注入时 Spring 直接启动失败）。解法是约定"**跨模块只依赖对方的 Mapper**"——数据访问层没有业务逻辑，天然不会绕环。

> 为什么不用一条 JOIN SQL 做详情？关联点只有一个（班级名）、调用频率低，两次简单查询代码最直观；关联多、列表批量回显的场景才值得上 JOIN。

## 五、联调实录

15 个用例实测（班级 11 + 学生回归 4）：

| # | 用例 | 请求 | 实测响应（核心） |
|---|---|---|---|
| C1 | 班级列表 | `GET /clazzes` | `studentCount: 2`（LEFT JOIN 统计生效） |
| C2 | 班级详情 | `GET /clazzes/1` | 2023级软件1班 |
| C3 | 不存在班级 | `GET /clazzes/999` | `{"code":3001,"message":"班级不存在"}` |
| C4 | 新增班级 | `POST /clazzes` | `{"code":200,"message":"新增成功","data":2}` |
| C5 | 缺班级名 | `POST /clazzes` 传 `{}` | `{"code":400,"message":"clazzName: 班级名称不能为空"}` |
| C6 | 修改班级 | `PUT /clazzes/2` | `code=200` |
| C7 | 修改不存在班级 | `PUT /clazzes/999` | `code=3001` |
| C8 | **删有学生的班** | `DELETE /clazzes/1` | `{"code":3002,"message":"该班级下还有 2 名学生，请先转移学生后再删除"}` |
| C9 | 删空班级 | `DELETE /clazzes/2` | `code=200` |
| C10 | 重复删已删班级 | `DELETE /clazzes/2` | `code=3001`（逻辑删除后查不到） |
| C11 | 列表回归 | `GET /clazzes` | 仅剩班级 1，人数仍为 2 |
| S1 | **学生详情回显** | `GET /students/1` | 多出 `"clazzName":"2023级软件1班"` |
| S2 | 学生列表回归 | `GET /students` | 正常分页 |
| S3 | 重复学号回归 | `POST /students` | `{"code":2002,"message":"学号 2023001 已存在"}` |
| S4 | 缺 age 校验 | `POST /students` | `{"code":400,"message":"age: age不能为空"}` |

> S4 对应实体规则的一次演进：`age` 从"选填"收紧为"必填"（加了 `@NotNull`）——校验规则随业务调整是常态，注解改一行即可，Service/Controller 零改动，这正是把校验挂在实体上的好处。

自定义 SQL 的控制台日志（统计查询真实执行形态）：

```sql
SELECT c.id, c.clazz_name, c.grade, c.head_teacher, c.description, c.create_time, c.update_time,
       COUNT(s.id) AS student_count
FROM clazz c
LEFT JOIN student s ON s.clazz_id = c.id AND s.deleted = 0
WHERE c.deleted = 0
GROUP BY c.id
ORDER BY c.id DESC
```

## 六、高频坑位

1. **自定义 SQL 忘写 `deleted=0`**：框架的逻辑删除拼接不覆盖 `@Select`/XML，写了自定义 SQL 就要自己负责全部过滤条件——写漏不会报错，只会查出"已删除的幽灵数据"；
2. **LEFT JOIN 的过滤条件放错位置**：对被连接表的过滤写在 `WHERE` 里会让左连接退化成内连接（见第二节），记住口诀"**连表条件放 ON，全局条件放 WHERE**"；
3. **Service 循环依赖**：两个模块互相需要时注入对方的 **Mapper** 而不是 Service；真要在 Service 间调用且无法避免，才考虑 `@Lazy` 延迟注入——但先想想是不是分层有问题；
4. **`BeanUtils.copyProperties(源, 目标)`**：Spring 的这个工具参数顺序和 Apache Commons 的正好相反，抄错方向不报错只是拷不过去，运行期才发现字段全 null；
5. **拿外键报错当业务提示**：FK 冲突的原始消息是给 DBA 看的，面向用户的校验永远先在业务层做，数据库约束只当最后防线；
6. **统计口径要和删除策略一致**：人数统计只数 `deleted=0` 的学生，否则逻辑删除一个学生，班级人数却不减——用户会以为删除没生效。

## 七、下一步预告

**第六步**：课程模块 CRUD——再次复用四件套模板收尾业务模块（`course` 表带 `teacher_id` 外键指向 `sys_user`，授课教师姓名回显是关联处理③的翻版）；随后转入**登录认证**：`sys_user` 密码加密存储、JWT 签发与拦截器鉴权，系统从"裸奔接口"进入"按角色访问"阶段。
