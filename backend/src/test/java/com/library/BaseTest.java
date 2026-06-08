package com.library;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 测试基类
 * 所有测试类可以继承此类获取通用配置
 *
 * @author Library System
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {
}
