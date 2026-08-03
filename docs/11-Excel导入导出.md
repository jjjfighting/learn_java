# 学生管理系统 · 第十一步：Excel 导入导出（EasyExcel）

> 本篇完成 EasyExcel 实战的两个典型场景：① **导出**——把某门课程的成绩单导出成带样式表头的 Excel；② **导入**——把学生数据从 Excel 批量导入，逐行校验、错误行 JSON 回报。全部用例需在你本地联调确认（检查单见第六节）。

## 一、为什么用 EasyExcel

Excel 读写有三条路，对比后选 EasyExcel：

| 方案 | 优劣 |
|---|---|
| **EasyExcel（本篇采用）** | 注解驱动（`@ExcelProperty` 声明列名），读是流式的、写是开箱即用的；复杂场景（样式、监听器、并发）都有成熟方案；社区资料最多 |
| Apache POI 原生 | 最底层、最灵活，但创建表头样式、逐行写单元格、处理日期全要手写样板代码，教学成本高 |
| Hutool ExcelUtil | 上手最快，但样式定制、逐行校验、错误收集能力弱 |

依赖一行（`pom.xml`）：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>3.3.4</version>
</dependency>
```

## 二、导出：课程成绩单

### 请求

```
GET /scores/export?courseId=1        （需登录，仅 TEACHER / ADMIN）
```

导出暴露该课程**所有学生**的成绩，比单条查询敏感，所以不放行公开白名单，并用 `@RequireRole({"TEACHER", "ADMIN"})` 门禁；同时挂 `@OperationLog` 留痕（导出算敏感读操作）。

### 数据从哪来

直接复用成绩模块已有的 JOIN 查询 `ScoreService.listScores(null, courseId)`——它已经回显了学号、姓名、课程名，零成本喂给导出。

### 行映射：`ScoreExportRow`

```java
@Data
public class ScoreExportRow {
    @ExcelProperty("学号")
    private String studentNo;
    @ExcelProperty("姓名")
    private String studentName;
    @ExcelProperty("分数")
    private BigDecimal score;
    @ExcelProperty("考试日期")
    private LocalDate examTime;

    public static ScoreExportRow from(ScoreVO vo) { ... }   // 从查询 VO 拷四列
}
```

`@ExcelProperty("学号")` 的 value 就是 Excel 表头列名，顺序与字段声明一致——**列名和顺序都由此注解决定**，前端不用写任何"哪列对应哪个字段"的映射逻辑。

### 表头样式：`HorizontalCellStyleStrategy`

EasyExcel 把"表头样式"和"数据行样式"拆成一对，用策略对象一次性注册：

```java
WriteCellStyle headStyle = new WriteCellStyle();
headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());  // 灰底
headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);                 // 水平居中
headStyle.setVerticalAlignment(VerticalAlignment.CENTER);                     // 垂直居中
headStyle.setBorderLeft(BorderStyle.THIN); ...                                // 细边框四边
WriteFont headFont = new WriteFont();
headFont.setBold(true);                                                       // 加粗
headStyle.setWriteFont(headFont);
HorizontalCellStyleStrategy strategy = new HorizontalCellStyleStrategy(headStyle, new WriteCellStyle());
```

然后流式写出到响应：

```java
try (ExcelWriter writer = EasyExcel.write(response.getOutputStream(), ScoreExportRow.class)
        .registerWriteHandler(strategy)      // 注册表头样式
        .build()) {
    WriteSheet sheet = EasyExcel.writerSheet("成绩单").build();
    writer.write(rows, sheet);               // rows 就是 List<ScoreExportRow>
}
```

响应头两个关键点：Excel 的 MIME 类型 + 中文文件名走 RFC 5987（和第十步下载接口同一套路）：

```java
response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
String fileName = URLEncoder.encode("成绩单_" + course.getCourseName() + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
```

## 三、导入：批量学生

### 请求

```
POST /students/import         （需登录，仅 ADMIN；multipart 表单字段名 file）
```

批量建人影响面大，故仅 ADMIN。模板表头**必须严格**是：

| 学号 | 姓名 | 性别 | 年龄 | 班级 |
|---|---|---|---|---|
| 2023003 | 王五 | 男 | 20 | 2023级软件1班 |

### 行映射：`StudentImportRow`——刻意全用 String

```java
@Data
public class StudentImportRow {
    @ExcelProperty("学号")
    private String studentNo;
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("性别")
    private String gender;
    @ExcelProperty("年龄")
    private String age;
    @ExcelProperty("班级")
    private String clazzName;
}
```

**为什么性别、年龄不用 Integer 接？** 一旦单元格里写了"abc"，EasyExcel 抛类型转换异常、**中断整次读取**，"错误行回报"就成了泡影。用 String 先接住，在 Service 里逐行手工解析校验——错误能按行收集，而不是整批崩溃。

### 读取

```java
List<StudentImportRow> rows = EasyExcel.read(in).head(StudentImportRow.class).sheet().doReadSync();
```

`doReadSync()` 同步把第一个 sheet 的所有数据行读成一个 `List`，实现最直白，适合教学；数据量大时再换 `AnalysisEventListener` 逐行流式处理。

### 逐行校验规则

| 列 | 规则 | 报错文案 |
|---|---|---|
| 学号 | 必填；≤20 字符（表列 VARCHAR(20)） | 学号不能为空 / 学号不能超过 20 个字符 |
| 学号唯一 | 库内唯一 + 文件内不重复 | 学号已存在：xxx / 学号在文件内重复：xxx |
| 姓名 | 必填；≤50 字符 | 姓名不能为空 / 姓名不能超过 50 个字符 |
| 性别 | 男/1→1，女/0→0；留空→null（数据库默认1） | 性别只能为 男/女 或 1/0 |
| 年龄 | 可选；0~150 的整数 | 年龄必须是数字 / 年龄需在 0~150 之间 |
| 班级 | 按名称查 `clazz` 表换 `clazzId`；留空→未分班 | 班级不存在：xxx |

### 策略：部分成功

合法行直接 `save` 落库，错误行收集进结果，**不中断整批导入**。整行为空的行跳过不计数（Excel 末尾常有多余空行）。

返回结构：

```json
{
  "code": 200,
  "data": {
    "totalRows": 10,
    "successCount": 8,
    "failCount": 2,
    "errors": [
      { "rowNum": 3, "content": "2023001,张三,,19,2023级软件1班", "reason": "性别只能为 男/女 或 1/0" },
      { "rowNum": 5, "content": "2023002,李四,0,150,不存在的班级", "reason": "班级不存在：不存在的班级" }
    ]
  }
}
```

`rowNum` 从 **2** 开始（第 1 行是表头），用户照着 Excel 能直接定位到出错行。

### 错误处理

- 空文件 → `5001 上传文件不能为空`
- 非 `.xlsx/.xls` → `400 请上传 .xlsx / .xls 格式的 Excel 文件`
- 文件损坏/不是真 Excel → 读取抛运行时异常，转 `400 Excel 文件格式不正确...`
- 文件解析的 `IOException` → `500 Excel 文件读取失败`

## 四、改动清单

```
pom.xml                                  修改：加 EasyExcel 3.3.4
dto/StudentImportRow.java                新增：导入行映射（全 String）
vo/ScoreExportRow.java                   新增：导出行映射
vo/StudentImportResultVO.java            新增：导入结果
vo/ImportErrorRowVO.java                 新增：错误行
service/StudentService.java              修改：加 importStudents
service/impl/StudentServiceImpl.java     修改：实现逐行校验+落库
controller/StudentController.java        修改：加 POST /import
controller/ScoreController.java          修改：加 GET /export
```

## 五、关键设计决策

1. **全 String 接列、Service 手工解析**——把 EasyExcel 的类型转换异常挡在读入阶段之外，逐行错误才能按行回报；
2. **部分成功而非全或无**——合法行先落库，错误行回报让用户只改错行、重传局部，不用整个文件重来；
3. **表头即契约**——`@ExcelProperty("学号")` 决定列映射，模板表头必须逐字匹配，否则该列全为 null、整行校验失败；
4. **复用 JOIN 查询**——导出不新写 SQL，直接吃 `listScores(courseId)`；
5. **读敏感挂审计**——导出成绩单挂 `@OperationLog(action="EXPORT")`，与上传/增删改同级留痕。

## 六、联调检查单（在你的环境实测）

> 以下为预期结果，请起服务后逐条验证。

| # | 用例 | 预期结果 |
|---|---|---|
| E1 | TEACHER 调 `GET /scores/export?courseId=1` | 下载到"成绩单_Java程序设计.xlsx"，打开有 4 列表头（灰底加粗）、数据与库中一致 |
| E2 | 不存在的 courseId 导出 | `code=3003` 课程不存在 |
| E3 | STUDENT 角色导出 | `code=403` |
| I1 | ADMIN 上传含 8 行合法 + 2 行非法的模板 | `successCount=8, failCount=2`，errors 里有行号/内容/原因；库中多 8 个学生 |
| I2 | 模板里学号重复（文件内） | 该行报"学号在文件内重复" |
| I3 | 模板里学号与库中已有重复 | 该行报"学号已存在" |
| I4 | 模板里班级名称不存在 | 该行报"班级不存在：xxx" |
| I5 | 上传非 Excel 文件 | `code=400` |
| I6 | 非 ADMIN 上传 | `code=403` |
| I7 | 导出/导入后查 op_log | 各有 1 条 `module=score,action=EXPORT` / `module=student,action=IMPORT` 日志 |

## 七、高频坑位

1. **表头不匹配，全行校验失败**：`@ExcelProperty` 的 value 与 Excel 表头必须逐字一致（含空格）。表头写了"学生姓名"，注解写"姓名"，该列就读不到 → 每行报"姓名不能为空"。用我们的模板原样填数即可。
2. **导入列直接声明成 Integer**：单元格写"abc"或空字符串会让 EasyExcel 抛转换异常中断读取，错误行回报失效。接收列一律 String，Service 里手工解析。
3. **行号搞错**：`List` 下标从 0 开始，Excel 数据行从第 2 行开始，回报给用户的行号 = 下标 + 2。
4. **忘记 `doReadSync`**：不带 `Sync` 的 `doRead` 需要配监听器，很多教程两种混着抄会踩空。读取全量列表用 `doReadSync()`。
5. **POI 依赖冲突**：EasyExcel 自带 POI；若项目再引其他版本 POI，可能出现类冲突，报错优先看 `ClassNotFoundException: org.apache.poi.*`。

## 八、下一步预告

**第十二步**：Redis 缓存热门查询 + Spring `@Scheduled` 定时任务，把进阶五件套收尾——届时把第十步留下的"清理孤儿文件"用定时任务补上。
