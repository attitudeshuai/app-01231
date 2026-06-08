package com.library.common.xss;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XSS防护配置类
 * 注册XSS过滤器
 *
 * @author Library System
 * @since 1.0.0
 */
@Configuration
public class XssConfig {

    /**
     * 配置XSS过滤器
     *
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter());
        // 过滤所有路径
        registration.addUrlPatterns("/*");
        // 设置过滤器名称
        registration.setName("xssFilter");
        // 设置过滤器顺序，确保在其他过滤器之前执行
        registration.setOrder(1);
        // 配置排除的URL（不需要XSS过滤的路径）
        // 例如：文件上传、富文本编辑器等可能包含HTML内容的接口
        registration.addInitParameter("excludeUrls", 
            "/swagger-ui/**," +
            "/v3/api-docs/**," +
            "/swagger-resources/**," +
            "/webjars/**," +
            "/uploads/**");
        return registration;
    }
}
