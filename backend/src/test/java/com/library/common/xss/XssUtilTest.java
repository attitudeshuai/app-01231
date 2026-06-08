package com.library.common.xss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XssUtil 单元测试
 *
 * @author Library System
 * @since 1.0.0
 */
@DisplayName("XSS工具类单元测试")
class XssUtilTest {

    @Test
    @DisplayName("XSS清理 - 基本HTML标签转义")
    void clean_BasicHtmlTags() {
        String input = "<script>alert('xss')</script>";
        String result = XssUtil.clean(input);
        
        assertNotNull(result);
        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
    }

    @Test
    @DisplayName("XSS清理 - 转义特殊字符")
    void clean_SpecialCharacters() {
        String input = "<div onclick=\"alert('xss')\">test</div>";
        String result = XssUtil.clean(input);
        
        assertNotNull(result);
        assertFalse(result.contains("<div"));
        assertTrue(result.contains("&lt;"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("XSS清理 - 空值处理")
    void clean_NullAndEmpty(String input) {
        String result = XssUtil.clean(input);
        assertEquals(input, result);
    }

    @Test
    @DisplayName("XSS清理 - 正常文本不变")
    void clean_NormalText() {
        String input = "这是一段正常的文本，包含中文和English";
        String result = XssUtil.clean(input);
        
        assertEquals(input, result);
    }

    @Test
    @DisplayName("XSS还原 - 还原转义字符")
    void unescape_Basic() {
        String input = "&lt;script&gt;alert('xss')&lt;/script&gt;";
        String result = XssUtil.unescape(input);
        
        assertEquals("<script>alert('xss')</script>", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("XSS还原 - 空值处理")
    void unescape_NullAndEmpty(String input) {
        String result = XssUtil.unescape(input);
        assertEquals(input, result);
    }

    @Test
    @DisplayName("移除危险内容 - script标签")
    void stripDangerous_ScriptTag() {
        String input = "Hello<script>alert('xss')</script>World";
        String result = XssUtil.stripDangerous(input);
        
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("</script>"));
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("World"));
    }

    @Test
    @DisplayName("移除危险内容 - 事件属性")
    void stripDangerous_EventAttributes() {
        String input = "<div onclick=\"alert('xss')\">test</div>";
        String result = XssUtil.stripDangerous(input);
        
        assertFalse(result.contains("onclick"));
    }

    @Test
    @DisplayName("移除危险内容 - javascript协议")
    void stripDangerous_JavascriptProtocol() {
        String input = "<a href=\"javascript:alert('xss')\">click</a>";
        String result = XssUtil.stripDangerous(input);
        
        assertFalse(result.toLowerCase().contains("javascript:"));
    }

    @Test
    @DisplayName("移除危险内容 - expression表达式")
    void stripDangerous_Expression() {
        String input = "<div style=\"width:expression(alert('xss'))\">test</div>";
        String result = XssUtil.stripDangerous(input);
        
        assertFalse(result.toLowerCase().contains("expression("));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("移除危险内容 - 空值处理")
    void stripDangerous_NullAndEmpty(String input) {
        String result = XssUtil.stripDangerous(input);
        assertEquals(input, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert('xss')</script>",
        "<div onclick=\"alert('xss')\">",
        "javascript:alert('xss')",
        "<img onerror=\"alert('xss')\">",
        "<a href=\"javascript:void(0)\">"
    })
    @DisplayName("检测XSS - 包含危险内容")
    void containsXss_True(String input) {
        assertTrue(XssUtil.containsXss(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "正常文本",
        "Hello World",
        "This is a test",
        "数字123和字母abc"
    })
    @DisplayName("检测XSS - 正常内容")
    void containsXss_False(String input) {
        assertFalse(XssUtil.containsXss(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("检测XSS - 空值处理")
    void containsXss_NullAndEmpty(String input) {
        assertFalse(XssUtil.containsXss(input));
    }

    @ParameterizedTest
    @CsvSource({
        "SELECT * FROM users, ' * FROM users'",
        "DROP TABLE users, ' TABLE users'",
        "INSERT INTO table, ' INTO table'",
        "UPDATE users SET, ' users SET'",
        "DELETE FROM users, ' FROM users'"
    })
    @DisplayName("SQL注入清理 - 移除关键字")
    void cleanSqlInjection_RemoveKeywords(String input, String expectedContains) {
        String result = XssUtil.cleanSqlInjection(input);
        
        assertNotNull(result);
        // 验证SQL关键字被移除
        assertFalse(result.toLowerCase().contains("select"));
        assertFalse(result.toLowerCase().contains("drop"));
        assertFalse(result.toLowerCase().contains("insert"));
        assertFalse(result.toLowerCase().contains("update"));
        assertFalse(result.toLowerCase().contains("delete"));
    }

    @Test
    @DisplayName("SQL注入清理 - union注入")
    void cleanSqlInjection_Union() {
        String input = "1 UNION SELECT * FROM users";
        String result = XssUtil.cleanSqlInjection(input);
        
        assertFalse(result.toLowerCase().contains("union"));
        assertFalse(result.toLowerCase().contains("select"));
    }

    @Test
    @DisplayName("SQL注入清理 - 注释注入")
    void cleanSqlInjection_Comments() {
        String input = "admin'-- comment";
        String result = XssUtil.cleanSqlInjection(input);
        
        assertFalse(result.contains("--"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("SQL注入清理 - 空值处理")
    void cleanSqlInjection_NullAndEmpty(String input) {
        String result = XssUtil.cleanSqlInjection(input);
        assertEquals(input, result);
    }

    @Test
    @DisplayName("SQL注入清理 - 正常文本不变")
    void cleanSqlInjection_NormalText() {
        String input = "这是一段正常的图书描述";
        String result = XssUtil.cleanSqlInjection(input);
        
        assertEquals(input, result);
    }

    @Test
    @DisplayName("XSS清理 - 嵌套script标签")
    void clean_NestedScriptTags() {
        String input = "<scr<script>ipt>alert('xss')</scr</script>ipt>";
        String result = XssUtil.clean(input);
        
        assertFalse(result.contains("<script>"));
    }

    @Test
    @DisplayName("XSS清理 - 大小写混合")
    void clean_MixedCase() {
        String input = "<ScRiPt>alert('xss')</sCrIpT>";
        String result = XssUtil.clean(input);
        
        assertTrue(result.contains("&lt;"));
    }

    @Test
    @DisplayName("XSS清理 - IMG标签onerror")
    void clean_ImgOnerror() {
        String input = "<img src=x onerror=alert('xss')>";
        String result = XssUtil.clean(input);
        
        assertTrue(result.contains("&lt;"));
        assertFalse(result.contains("<img"));
    }

    @Test
    @DisplayName("XSS清理 - SVG标签")
    void clean_SvgTag() {
        String input = "<svg onload=alert('xss')>";
        String result = XssUtil.clean(input);
        
        assertTrue(result.contains("&lt;"));
        assertFalse(result.contains("<svg"));
    }

    @Test
    @DisplayName("移除危险内容 - onmouseover事件")
    void stripDangerous_OnMouseOver() {
        String input = "<div onmouseover=\"alert('xss')\">hover me</div>";
        String result = XssUtil.stripDangerous(input);
        
        assertFalse(result.toLowerCase().contains("onmouseover"));
    }

    @Test
    @DisplayName("移除危险内容 - onfocus事件")
    void stripDangerous_OnFocus() {
        String input = "<input onfocus=\"alert('xss')\" autofocus>";
        String result = XssUtil.stripDangerous(input);
        
        assertFalse(result.toLowerCase().contains("onfocus"));
    }
}
