-- =====================================================
-- 第十步迁移脚本：文件表 sys_file
-- 在已有数据库上执行（init.sql 已同步包含该表，全新建库无需跑本脚本）
-- =====================================================
USE student_ms;

-- 文件表：记录上传文件的元数据（不存文件本身，文件在磁盘 uploads/ 目录）
-- 有了这张表，前端拿到文件 ID 即可随时重新下载，不再依赖磁盘名拼 URL；
-- 同时保住了原始文件名 / 大小 / 类型 / 上传人，丢失上传响应也能找回文件。
-- 表名用 sys_file 而非 file：避免与 java.io.File 撞名、规避 MySQL FILE 关键字歧义。
CREATE TABLE IF NOT EXISTS sys_file (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    original_name VARCHAR(255)  NOT NULL                COMMENT '原始文件名（下载/展示用）',
    stored_name   VARCHAR(64)   NOT NULL                COMMENT '磁盘存储名：UUID.扩展名',
    content_type  VARCHAR(100)  NOT NULL                COMMENT '文件 MIME 类型',
    size          BIGINT        NOT NULL                COMMENT '文件大小（字节）',
    uploaded_by   BIGINT        DEFAULT NULL            COMMENT '上传人ID（外键 -> sys_user.id，允许为空）',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    deleted       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stored_name (stored_name),
    KEY idx_uploaded_by (uploaded_by),
    CONSTRAINT fk_file_uploader
        FOREIGN KEY (uploaded_by) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件表';
