package com.studentms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 冒烟测试：验证 Spring 容器能正常启动（所有 Bean 都能正确装配）
 */
@SpringBootTest
class StudentMsApplicationTests {

    @Test
    void contextLoads() {
        // 方法体为空即可：只要项目配置有错，这个测试就会启动失败并报错
    }
}
