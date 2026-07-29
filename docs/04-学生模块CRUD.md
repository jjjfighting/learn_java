# 学生管理系统 · 第四步：学生模块 CRUD

> 本篇完成：按经典四层结构（实体 / Mapper / Service / Controller）开发学生模块，5 个 RESTful 接口全部用 `Result` 包装、用 `BusinessException` 表达业务错误，`createTime`/`updateTime` 自动填充生效，并用 16 个 curl 用例做了完整接口联调（含异常路径）。

## 一、分层架构总览

```
浏览器/Postman
    │  HTTP JSON
    ▼
Controller 层   StudentController      收参数、做校验、包装 Result，不写业务逻辑
    ▼
Service 层      StudentService(Impl)   业务逻辑：查重、存在性判断、抛业务异常
    ▼
Mapper 层       StudentMapper          数据库访问，继承 BaseMapper 免写 SQL
    ▼
MySQL           student 表
```

为什么要分层？一句话：**每层只干一件事，改其中一层不影响其他层**。换数据库只动 Mapper，改业务规则只动 Service，调接口格式只动 Controller。

本步新增文件：

```
src/main/java/com/studentms/
├── entity/Student.java               # 实体类（表映射 + 参数校验注解）
├── mapper/StudentMapper.java         # 数据访问层（接口，无实现类）
├── dto/StudentQueryDTO.java          # 分页查询条件对象
├── service/
│   ├── StudentService.java           # 业务接口
│   └── impl/StudentServiceImpl.java  # 业务实现
└── controller/StudentController.java # RESTful 接口
```

另外改了两处：`MybatisPlusConfig` 加 `@MapperScan`；`GlobalExceptionHandler` 增加数据库约束异常处理（见第六节）。

## 二、实体类 Student

```java
@Data
@TableName("student")
public class Student implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    ...
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @JsonIgnore
    private Integer deleted;
}
```

注解逐个讲：

| 注解 | 作用 | 不写会怎样 |
|---|---|---|
| `@TableName("student")` | 指定表名 | 类名 `Student` 恰好能推出表名 `student`，其实可省略；写上是明确表达意图 |
| `@TableId(type = IdType.AUTO)` | 标记主键 + 跟随数据库自增 | yml 里已全局配置 `id-type: auto`，这里显式写出更清晰 |
| `@TableField(fill = FieldFill.INSERT)` | 声明该字段新增时自动填充 | 第三步的 `MyMetaObjectHandler` 永远等不到回调，时间字段为 null |
| `@TableField(fill = FieldFill.INSERT_UPDATE)` | 新增和更新时都自动填充 | 更新后 `update_time` 不再变化（除非靠数据库 `ON UPDATE`） |
| `@JsonIgnore` | 序列化 JSON 时跳过该字段 | 前端会看到 `"deleted": 0` 这种内部实现细节 |
| `@NotBlank` / `@Min` / `@Email` | 参数校验（配合 Controller 的 `@Valid`） | 非法参数直接进业务层甚至数据库 |

**两个设计取舍**：

1. **`deleted` 不用写 `@TableLogic`**：第二步在 yml 里全局配置了 `logic-delete-field: deleted`，所有实体自动生效。联调 SQL 日志为证——每条查询都自动带了尾巴：

   ```sql
   SELECT ... FROM student WHERE id=? AND deleted=0
   ```

2. **校验注解直接写在实体上**：严格的企业项目会另建 `StudentAddDTO` / `StudentUpdateDTO` 分别定义校验规则（新增必填、修改可选）。本项目新增和修改的必填项恰好一致（学号 + 姓名），复用实体更简洁，`age`/`email` 等字段"允许为空、填了就必须合法"的规则用 `@Min`/`@Email` 天然表达。

## 三、Mapper 层：一个没有实现类的接口

```java
public interface StudentMapper extends BaseMapper<Student> {
}
```

**没有实现类，却能直接注入使用**，原理分两半：

1. **SQL 从哪来**：MyBatis-Plus 启动时解析 `Student` 实体上的注解，为 `BaseMapper` 的每个方法（`insert`/`selectById`/`updateById`/`selectPage`...）动态生成 SQL，注入 MyBatis；
2. **对象从哪来**：MyBatis 用 **JDK 动态代理**为这个接口生成运行时代理对象——你调用任何方法，代理拦截后找到对应的预生成 SQL 去执行；`@MapperScan("com.studentms.mapper")` 再把这些代理对象注册进 Spring 容器。

所以"接口没有实现类"只是看起来没有——实现类是运行时凭空造出来的。以后复杂查询需要自定义 SQL 时，在这个接口里加 `@Select` 注解方法或配 XML 即可。

`@MapperScan` 写在 `MybatisPlusConfig` 上，一次性扫描整个包——以后每个新模块的 Mapper 接口放进去就行，不用再逐个标注。

## 四、Service 层：业务逻辑的家

```java
public interface StudentService extends IService<Student> { ... }

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student>
        implements StudentService { ... }
```

- `IService<Student>` / `ServiceImpl<M, T>` 是 MyBatis-Plus 的"通用业务层"：`save` / `getById` / `updateById` / `removeById` / `page` / `lambdaQuery()` 全部现成；
- 我们只写**带业务规则**的方法，本模块有三条规则：

**规则一：学号唯一。** 新增前查库：

```java
if (existsByStudentNo(student.getStudentNo(), null)) {
    throw new BusinessException(ResultCode.STUDENT_NO_ALREADY_EXISTS,
            "学号 " + student.getStudentNo() + " 已存在");
}
```

注意抛异常时把学号带进提示语——前端弹"学号 2023003 已存在"比"学号已存在"友好得多。

**规则二：修改查重要排除自己。** 否则"只改了名字、学号没变"会被误判为学号重复：

```java
return this.lambdaQuery()
        .eq(Student::getStudentNo, studentNo)
        .ne(excludeId != null, Student::getId, excludeId)   // 修改时排除自身
        .exists();
```

**规则三：改/删/查详情前先判断存在性**，不存在就抛 `STUDENT_NOT_FOUND(2001)`，而不是对着 null 继续操作。

**条件查询的写法**也值得记：

```java
LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
        .eq(StringUtils.hasText(query.getStudentNo()), Student::getStudentNo, query.getStudentNo())
        .like(StringUtils.hasText(query.getName()), Student::getName, query.getName())
        .eq(query.getClazzId() != null, Student::getClazzId, query.getClazzId())
        .orderByDesc(Student::getId);
```

每行第一个布尔参数决定"这个条件拼不拼进 SQL"——用户没填的查询项不能变成 `name LIKE '%%'` 之类的恒真条件。用 `Lambda` 版（方法引用 `Student::getName`）而不是字符串版（`"name"`），是因为字段改名时编译期就能发现。

## 五、Controller 层：RESTful 风格

| 方法 | URL | 说明 | 返回 data |
|---|---|---|---|
| GET | `/students?name=张&pageNum=1&pageSize=10` | 分页条件查询 | `Page<Student>` |
| GET | `/students/{id}` | 详情 | `Student` |
| POST | `/students` | 新增 | 新记录自增 ID |
| PUT | `/students/{id}` | 修改 | null |
| DELETE | `/students/{id}` | 删除（逻辑删除） | null |

RESTful 的核心约定：**URL 只表示资源（名词复数），动作交给 HTTP 方法**。五个接口共享一个 `/students` 路径，比 `/getStudent`、`/addStudent`、`/deleteStudent` 这种动词风格更整齐，前端也更好猜。

两个细节：

1. **构造器注入**：类上 `@RequiredArgsConstructor` + 字段 `private final StudentService`，Lombok 生成构造器完成注入。官方不推荐 `@Autowired` 字段注入：依赖不可变、便于单测、缺依赖时启动即失败而不是运行期 NPE；
2. **新增成功返回 ID**：`save()` 后自增主键被 MyBatis-Plus 回填进实体，顺手返回给前端，省一次查询就能跳详情页。

## 六、公共层补充：数据库约束异常

联调时发现一个真实场景：新增学生时 `clazzId` 填了不存在的班级，数据库外键约束拒绝插入，异常一路冒到兜底处理器返回 500。但这是**客户端传错数据**，应该是 400。于是在 `GlobalExceptionHandler` 补了一个处理方法：

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
    log.warn("数据约束冲突：{}", e.getMessage());
    return Result.error(ResultCode.BAD_REQUEST, "数据不满足约束：关联的班级或账号可能不存在，或字段值非法");
}
```

外键、唯一键、NOT NULL 冲突都会被 Spring 翻译成 `DataIntegrityViolationException`，一个处理器全覆盖。

## 七、接口联调实录

启动项目后用 curl 实测 16 个用例（Windows Git Bash 下中文参数会被转成 GBK，**含中文的请求体要写进文件用 `-d @文件` 传入，URL 里的中文要手工编码**，如 `?name=%E5%BC%A0`）：

| # | 用例 | 请求 | 实测响应（核心） |
|---|---|---|---|
| 1 | 列表 | `GET /students` | `code=200`，total=2，张三李四 |
| 2 | 姓名模糊查 | `GET /students?name=%E5%BC%A0` | total=1，仅张三 |
| 3 | 翻第二页 | `GET /students?pageSize=1&pageNum=2` | total=2、pages=2，返回张三 |
| 4 | 新增王五 | `POST /students` + JSON | `{"code":200,"message":"新增成功","data":3}` |
| 5 | 查详情 | `GET /students/3` | createTime/updateTime 已自动填充，无 deleted 字段 |
| 6 | 重复学号 | `POST` 同 2023003 | `{"code":2002,"message":"学号 2023003 已存在"}` |
| 7 | 班级不存在 | `POST` clazzId=999 | `{"code":400,"message":"数据不满足约束：..."}` |
| 8 | 邮箱非法 | `POST` email=not-an-email | `{"code":400,"message":"email: 邮箱格式不正确"}` |
| 9 | 修改（学号不变） | `PUT /students/3` 改名改龄 | `code=200`（排除自身查重生效） |
| 10 | 查修改结果 | `GET /students/3` | name=王五五、age=21 |
| 11 | 抢占他人学号 | `PUT` studentNo=2023001 | `{"code":2002,"message":"学号 2023001 已存在"}` |
| 12 | 删除 | `DELETE /students/3` | `code=200` |
| 13 | 删后查详情 | `GET /students/3` | `{"code":2001,"message":"学生不存在"}` |
| 14 | 列表回归 | `GET /students` | total 回到 2 |
| 15 | 查不存在 ID | `GET /students/999` | `code=2001` |
| 16 | 非法页码 | `GET /students?pageNum=0` | `{"code":400,"message":"pageNum: 页码从 1 开始"}` |

### SQL 日志证据（控制台 StdOutImpl 打印）

**① 自动填充**——`create_time`/`update_time` 出现在 INSERT 语句里，参数是 Java 侧生成的时间：

```sql
INSERT INTO student ( student_no, name, gender, age, phone, email, clazz_id, create_time, update_time )
VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ? )
-- Parameters: 2023003, 王五, 1, 20, 13800000003, wangwu@example.com, 1,
--             2026-07-28T16:45:20.968014600, 2026-07-28T16:45:20.968014600
```

**② 更新填充**——`update_time=?` 同样带参数：

```sql
UPDATE student SET student_no=?, name=?, gender=?, age=?, clazz_id=?, update_time=?
WHERE id=? AND deleted=0
```

**③ 逻辑删除**——`DELETE` 请求实际执行的是 UPDATE，且自动带上 `deleted=0` 防重复删：

```sql
UPDATE student SET update_time=?, deleted=1 WHERE id=? AND deleted=0
```

> 数据库里 id=3 的记录还在，只是 `deleted=1`，随时可以 UPDATE 回来——这就是逻辑删除的意义。重跑 `sql/init.sql` 可恢复全部初始数据。

## 八、高频坑位

1. **`@PathVariable` 报"parameter name not available"**：Spring 靠编译期保留的参数名匹配路径变量，javac 需要 `-parameters` 标志。Maven 构建由 `spring-boot-starter-parent` 默认开启，IDEA 导入 Maven 项目后自动继承；手动 javac 编译则必须自己加。保险写法是 `@PathVariable("id")` 显式指定；
2. **curl 中文乱码（Windows）**：Git Bash 把命令行中文参数转成 GBK 传给 curl.exe，服务端按 UTF-8 解析直接报 `Invalid UTF-8 middle byte`。解法：请求体写文件 `-d @body.json`，URL 中文手工百分号编码；Postman / IDEA HTTP Client 无此问题；
3. **时间精度"丢失"**：联调时发现 `updateTime` 和 `createTime` 显示同一秒——其实两次操作相隔几百毫秒，但数据库 `DATETIME` 默认秒级精度，小数部分被四舍五入。想保留毫秒要改成 `DATETIME(3)`；
4. **逻辑删除不触发数据库级联**：`score` 表的外键配了 `ON DELETE CASCADE`，但逻辑删除执行的是 UPDATE，数据库认为没有删除发生，成绩记录会保留。需要级联清理时要在 Service 里手动删，或将来改用物理删除接口；
5. **Service 里不要返回 null 表示失败**：本模块所有"找不到"一律抛 `STUDENT_NOT_FOUND`。返回 null 会把判空压力转嫁给每一个调用方，早晚炸成 NPE；
6. **查询条件忘记判空**：`.like(Student::getName, query.getName())` 在 name 为 null 时拼出恒真条件，等于无条件全表扫——务必用条件构造器的三参数重载（第一个参数是开关）。

## 九、下一步预告

**第五步**：班级模块 CRUD——结构与学生模块相同（模板已成型，速度会快很多），重点转向**关联处理**：班级列表带学生人数统计、删除班级时与外键约束（`fk_student_clazz` RESTRICT）的协作、学生详情回显班级名称。
