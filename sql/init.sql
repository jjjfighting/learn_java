-- =====================================================
-- 学生管理系统 数据库初始化脚本
-- 数据库: MySQL 8.x | 引擎: InnoDB | 字符集: utf8mb4
-- 执行顺序有讲究：先建"被引用"的表，再建"引用别人"的表
-- =====================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS student_ms
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;
USE student_ms;

-- 重新执行脚本时先删旧表（顺序与建表相反：先删引用别人的表）
DROP TABLE IF EXISTS score;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS clazz;
DROP TABLE IF EXISTS sys_user;

-- -----------------------------------------------------
-- 2. 用户表：系统所有登录账号（管理员 / 教师 / 学生）
-- -----------------------------------------------------
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL                COMMENT '登录用户名（唯一）',
    password    VARCHAR(100) NOT NULL                COMMENT '登录密码（后期加密存储）',
    real_name   VARCHAR(50)  DEFAULT NULL            COMMENT '真实姓名',
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT'
                             COMMENT '角色：ADMIN管理员 / TEACHER教师 / STUDENT学生',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '账号状态：0禁用 1启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                             COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表（系统登录账号）';

-- -----------------------------------------------------
-- 3. 班级表
-- -----------------------------------------------------
CREATE TABLE clazz (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    clazz_name   VARCHAR(50)  NOT NULL                COMMENT '班级名称，如：2023级软件1班',
    grade        VARCHAR(20)  DEFAULT NULL            COMMENT '入学年级，如：2023',
    head_teacher VARCHAR(50)  DEFAULT NULL            COMMENT '班主任姓名',
    description  VARCHAR(255) DEFAULT NULL            COMMENT '班级简介',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班级表';

-- -----------------------------------------------------
-- 4. 课程表（授课教师指向 sys_user 中角色为 TEACHER 的用户）
-- -----------------------------------------------------
CREATE TABLE course (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    course_name VARCHAR(100)  NOT NULL                COMMENT '课程名称',
    credit      DECIMAL(3, 1) DEFAULT NULL            COMMENT '学分，如：3.5',
    teacher_id  BIGINT        DEFAULT NULL            COMMENT '授课教师ID（外键 -> sys_user.id）',
    description VARCHAR(255)  DEFAULT NULL            COMMENT '课程简介',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_teacher_id (teacher_id),
    CONSTRAINT fk_course_teacher
        FOREIGN KEY (teacher_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程表';

-- -----------------------------------------------------
-- 5. 学生表（归属于班级，可选关联登录账号）
-- -----------------------------------------------------
CREATE TABLE student (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    student_no  VARCHAR(20) NOT NULL                COMMENT '学号（唯一，业务编号）',
    name        VARCHAR(50) NOT NULL                COMMENT '学生姓名',
    gender      TINYINT     NOT NULL DEFAULT 1      COMMENT '性别：0女 1男',
    age         TINYINT     DEFAULT NULL            COMMENT '年龄',
    phone       VARCHAR(20) DEFAULT NULL            COMMENT '联系电话',
    email       VARCHAR(100) DEFAULT NULL           COMMENT '电子邮箱',
    clazz_id    BIGINT      DEFAULT NULL            COMMENT '所属班级ID（外键 -> clazz.id）',
    user_id     BIGINT      DEFAULT NULL            COMMENT '关联账号ID（外键 -> sys_user.id，允许为空）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_clazz_id (clazz_id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_student_clazz
        FOREIGN KEY (clazz_id) REFERENCES clazz (id),          -- 默认 RESTRICT：班里还有学生时禁止删班
    CONSTRAINT fk_student_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生表';

-- -----------------------------------------------------
-- 6. 成绩表：学生 + 课程 的中间表（多对多拆解）
-- -----------------------------------------------------
CREATE TABLE score (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    student_id  BIGINT       NOT NULL                COMMENT '学生ID（外键 -> student.id）',
    course_id   BIGINT       NOT NULL                COMMENT '课程ID（外键 -> course.id）',
    score       DECIMAL(5, 2) DEFAULT NULL           COMMENT '分数，如：92.50',
    exam_time   DATE         DEFAULT NULL            COMMENT '考试日期',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    -- 保证"一名学生一门课只有一条成绩"，同时防止重复录入
    UNIQUE KEY uk_student_course (student_id, course_id),
    CONSTRAINT fk_score_student
        FOREIGN KEY (student_id) REFERENCES student (id)
        ON DELETE CASCADE,   -- 删除学生时，自动清掉他的所有成绩
    CONSTRAINT fk_score_course
        FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '成绩表';

-- =====================================================
-- 7. 初始化测试数据（可选）
-- =====================================================

-- 一个管理员、一名教师（密码先明文存 123456，登录功能时再改成加密）
INSERT INTO sys_user (username, password, real_name, role) VALUES
('admin',  '123456', '系统管理员', 'ADMIN'),
('t_wang', '123456', '王老师',     'TEACHER');

INSERT INTO clazz (clazz_name, grade, head_teacher) VALUES
('2023级软件1班', '2023', '王老师');

INSERT INTO course (course_name, credit, teacher_id) VALUES
('Java程序设计', 4.0, 2),
('MySQL数据库',  3.0, 2);

INSERT INTO student (student_no, name, gender, age, clazz_id) VALUES
('2023001', '张三', 1, 19, 1),
('2023002', '李四', 0, 18, 1);

INSERT INTO score (student_id, course_id, score, exam_time) VALUES
(1, 1, 92.50, '2026-01-10'),
(2, 1, 78.00, '2026-01-10');
