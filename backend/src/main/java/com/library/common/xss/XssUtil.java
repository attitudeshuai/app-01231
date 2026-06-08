package com.library.common.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;

import java.util.regex.Pattern;

/**
 * XSS工具类
 * 提供手动XSS过滤的方法，用于需要特殊处理的场景
 *
 * @author Library System
 * @since 1.0.0
 */
public class XssUtil {

    /**
     * 危险脚本模式
     */
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*?>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    
    private static final Pattern EVENT_PATTERN = Pattern.compile(
            "\\s*on\\w+\\s*=\\s*(['\"])[^'\"]*\\1", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
            "javascript\\s*:", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "expression\\s*\\([^)]*\\)", Pattern.CASE_INSENSITIVE);

    private XssUtil() {
        // 工具类不允许实例化
    }

    /**
     * 清除XSS脚本（HTML转义）
     *
     * @param value 原始字符串
     * @return 过滤后的字符串
     */
    public static String clean(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return HtmlUtil.escape(value);
    }

    /**
     * 还原被转义的HTML
     *
     * @param value 转义后的字符串
     * @return 还原后的字符串
     */
    public static String unescape(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return HtmlUtil.unescape(value);
    }

    /**
     * 移除危险的HTML标签和属性（不进行转义）
     * 适用于允许部分HTML但需要移除危险内容的场景
     *
     * @param value 原始字符串
     * @return 过滤后的字符串
     */
    public static String stripDangerous(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        
        String result = value;
        
        // 移除script标签
        result = SCRIPT_PATTERN.matcher(result).replaceAll("");
        
        // 移除事件属性
        result = EVENT_PATTERN.matcher(result).replaceAll("");
        
        // 移除javascript:协议
        result = JAVASCRIPT_PATTERN.matcher(result).replaceAll("");
        
        // 移除expression表达式
        result = EXPRESSION_PATTERN.matcher(result).replaceAll("");
        
        return result;
    }

    /**
     * 检查字符串是否包含XSS攻击代码
     *
     * @param value 待检查的字符串
     * @return 是否包含XSS攻击代码
     */
    public static boolean containsXss(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        
        return SCRIPT_PATTERN.matcher(value).find()
                || EVENT_PATTERN.matcher(value).find()
                || JAVASCRIPT_PATTERN.matcher(value).find()
                || EXPRESSION_PATTERN.matcher(value).find()
                || value.contains("<")
                || value.contains(">");
    }

    /**
     * 过滤SQL注入关键字
     *
     * @param value 原始字符串
     * @return 过滤后的字符串
     */
    public static String cleanSqlInjection(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        
        // 移除SQL注入关键字
        String[] sqlKeywords = {
            "select", "insert", "update", "delete", "drop", "truncate",
            "exec", "execute", "xp_", "sp_", "0x", "--", "/*", "*/",
            "union", "declare", "cast", "convert"
        };
        
        String result = value;
        for (String keyword : sqlKeywords) {
            // 使用正则匹配完整单词
            result = result.replaceAll("(?i)\\b" + keyword + "\\b", "");
        }
        
        return result;
    }
}
