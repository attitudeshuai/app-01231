package com.library.common.xss;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * XSS过滤器
 * 对所有请求进行XSS过滤，防止XSS攻击
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
public class XssFilter implements Filter {

    /**
     * 排除的URL路径列表
     */
    private List<String> excludeUrls = new ArrayList<>();

    /**
     * 排除的URL正则表达式列表
     */
    private List<Pattern> excludePatterns = new ArrayList<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 从配置中获取排除的URL
        String excludeUrlStr = filterConfig.getInitParameter("excludeUrls");
        if (StrUtil.isNotBlank(excludeUrlStr)) {
            String[] urls = excludeUrlStr.split(",");
            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (StrUtil.isNotBlank(trimmedUrl)) {
                    if (trimmedUrl.contains("*")) {
                        // 包含通配符，转换为正则表达式
                        String regex = trimmedUrl.replace("**", ".*").replace("*", "[^/]*");
                        excludePatterns.add(Pattern.compile(regex));
                    } else {
                        excludeUrls.add(trimmedUrl);
                    }
                }
            }
        }
        log.info("XSS过滤器初始化完成，排除路径: {}", excludeUrls);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();

        // 检查是否为排除的URL
        if (isExcludeUrl(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        // 使用XSS包装器包装请求
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(httpRequest);
        chain.doFilter(xssRequest, response);
    }

    /**
     * 判断是否为排除的URL
     *
     * @param requestUri 请求URI
     * @return 是否排除
     */
    private boolean isExcludeUrl(String requestUri) {
        // 精确匹配
        if (excludeUrls.contains(requestUri)) {
            return true;
        }
        
        // 正则匹配
        for (Pattern pattern : excludePatterns) {
            if (pattern.matcher(requestUri).matches()) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void destroy() {
        log.info("XSS过滤器销毁");
    }
}
