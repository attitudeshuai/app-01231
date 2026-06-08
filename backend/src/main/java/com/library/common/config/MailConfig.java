package com.library.common.config;

import com.library.common.service.MailService;
import com.library.common.service.impl.MailServiceImpl;
import com.library.common.service.impl.MockMailServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 邮件服务配置类
 *
 * <p>采用条件装配策略，根据运行环境自动选择邮件服务实现：</p>
 * <ul>
 *   <li>当配置了SMTP邮件服务（存在{@link JavaMailSender} Bean）时，
 *       使用{@link MailServiceImpl}发送真实邮件</li>
 *   <li>当未配置SMTP时，自动降级为{@link MockMailServiceImpl}，
 *       仅将邮件内容输出到日志，便于开发和测试</li>
 * </ul>
 *
 * <p>生产环境配置示例（application.yml）：</p>
 * <pre>
 * spring:
 *   mail:
 *     host: smtp.example.com
 *     port: 587
 *     username: your-email@example.com
 *     password: your-password
 * </pre>
 *
 * @author Library System
 * @since 1.0.0
 * @see MailService
 * @see MailServiceImpl
 * @see MockMailServiceImpl
 */
@Configuration
public class MailConfig {

    /**
     * 创建真实邮件服务Bean
     *
     * <p>当Spring容器中存在{@link JavaMailSender} Bean时激活，
     * 用于发送借阅到期提醒、逾期通知等邮件。</p>
     *
     * @param mailSender Spring Mail发送器
     * @return 真实邮件服务实现
     */
    @Bean
    @ConditionalOnBean(JavaMailSender.class)
    public MailService realMailService(JavaMailSender mailSender) {
        return new MailServiceImpl(mailSender);
    }

    /**
     * 创建Mock邮件服务Bean（降级方案）
     *
     * <p>当未配置SMTP邮件服务时自动启用，将邮件内容记录到日志。
     * 适用于开发环境和未配置邮箱的部署场景。</p>
     *
     * @return Mock邮件服务实现
     */
    @Bean
    @ConditionalOnMissingBean(MailService.class)
    public MailService mockMailService() {
        return new MockMailServiceImpl();
    }
}
