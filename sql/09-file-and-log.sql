-- =====================================================
-- 第九步迁移脚本：文件上传 + 操作日志
-- 在已有数据库上执行（init.sql 已同步包含这些结构，全新建库无需跑本脚本）
-- =====================================================
USE student_ms;

-- 1. 学生表增加头像列：存文件的访问路径（如 /files/view/xxx.png），不存文件本身
ALTER TABLE student
    ADD COLUMN photo VARCHAR(255) DEFAULT NULL COMMENT '头像访问路径' AFTER email;

-- 2. 操作日志表：只追加不修改，因此没有 update_time / deleted——
--    日志的价值就在于"发生过什么"，逻辑删除和更新都会破坏审计属性
CREATE TABLE IF NOT EXISTS op_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT        DEFAULT NULL            COMMENT '操作人ID（登录接口调用时可能为空）',
    username    VARCHAR(50)   DEFAULT NULL            COMMENT '操作人用户名',
    module      VARCHAR(30)   NOT NULL                COMMENT '模块：student/clazz/course/score/auth/file',
    action      VARCHAR(20)   NOT NULL                COMMENT '动作：CREATE/UPDATE/DELETE/LOGIN/UPLOAD 等',
    method      VARCHAR(10)   DEFAULT NULL            COMMENT 'HTTP 方法',
    path        VARCHAR(255)  DEFAULT NULL            COMMENT '请求路径',
    ip          VARCHAR(64)   DEFAULT NULL            COMMENT '客户端 IP',
    params      VARCHAR(1000) DEFAULT NULL            COMMENT '请求参数（已脱敏、截断）',
    result_code INT           DEFAULT NULL            COMMENT '响应业务码（失败时为对应错误码）',
    error_msg   VARCHAR(500)  DEFAULT NULL            COMMENT '异常消息（成功时为空）',
    cost_ms     BIGINT        DEFAULT NULL            COMMENT '接口耗时（毫秒）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    KEY idx_module_action (module, action),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';
