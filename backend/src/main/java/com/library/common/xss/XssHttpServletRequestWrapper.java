package com.library.common.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XSS请求包装器
 * 对请求参数和请求体进行XSS过滤
 *
 * @author Library System
 * @since 1.0.0
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 缓存的请求体
     */
    private byte[] body;

    public XssHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 缓存请求体
        body = request.getInputStream().readAllBytes();
    }

    /**
     * 过滤XSS脚本
     *
     * @param value 原始值
     * @return 过滤后的值
     */
    private String cleanXss(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        // 使用Hutool的HtmlUtil进行HTML转义
        return HtmlUtil.escape(value);
    }

    /**
     * 过滤请求参数
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    /**
     * 过滤请求参数值数组
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        int length = values.length;
        String[] escapedValues = new String[length];
        for (int i = 0; i < length; i++) {
            escapedValues[i] = cleanXss(values[i]);
        }
        return escapedValues;
    }

    /**
     * 过滤请求参数Map
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameterMap = super.getParameterMap();
        Map<String, String[]> escapedParameterMap = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            String[] escapedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                escapedValues[i] = cleanXss(values[i]);
            }
            escapedParameterMap.put(entry.getKey(), escapedValues);
        }
        return escapedParameterMap;
    }

    /**
     * 过滤请求头
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanXss(value);
    }

    /**
     * 获取过滤后的输入流
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 对JSON请求体进行XSS过滤
        String bodyStr = new String(body, StandardCharsets.UTF_8);
        String cleanedBody = cleanJsonXss(bodyStr);
        byte[] cleanedBytes = cleanedBody.getBytes(StandardCharsets.UTF_8);
        
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cleanedBytes);
        
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 不需要实现
            }

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * 对JSON字符串中的值进行XSS过滤
     * 只过滤字符串值，不破坏JSON结构
     *
     * @param json JSON字符串
     * @return 过滤后的JSON字符串
     */
    private String cleanJsonXss(String json) {
        if (StrUtil.isBlank(json)) {
            return json;
        }
        
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escape = false;
        StringBuilder currentString = new StringBuilder();
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escape) {
                if (inString) {
                    currentString.append(c);
                } else {
                    result.append(c);
                }
                escape = false;
                continue;
            }
            
            if (c == '\\') {
                escape = true;
                if (inString) {
                    currentString.append(c);
                } else {
                    result.append(c);
                }
                continue;
            }
            
            if (c == '"') {
                if (inString) {
                    // 字符串结束，进行XSS过滤
                    String cleaned = HtmlUtil.escape(currentString.toString());
                    result.append('"').append(cleaned).append('"');
                    currentString = new StringBuilder();
                    inString = false;
                } else {
                    // 字符串开始
                    inString = true;
                }
                continue;
            }
            
            if (inString) {
                currentString.append(c);
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}
