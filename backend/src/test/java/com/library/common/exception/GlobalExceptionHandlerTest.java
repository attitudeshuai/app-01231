package com.library.common.exception;

import com.library.common.response.Result;
import com.library.common.response.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");

        Result<Void> result = handler.handleBusinessException(new BusinessException(ResultCode.USER_NOT_EXIST), request);

        assertEquals(ResultCode.USER_NOT_EXIST.getCode(), result.getCode());
    }

    @Test
    void handleConstraintViolationException_Success() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("参数错误");

        Result<Void> result = handler.handleConstraintViolationException(new ConstraintViolationException(Set.of(violation)));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), result.getCode());
        assertEquals("参数错误", result.getMessage());
    }

    @Test
    void handleBindException_Success() {
        BindException exception = new BindException(new Object(), "form");
        exception.addError(new FieldError("form", "username", "不能为空"));

        Result<Void> result = handler.handleBindException(exception);

        assertEquals(ResultCode.PARAM_ERROR.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("username"));
    }

    @Test
    void handleMissingServletRequestParameterException_Success() throws Exception {
        Result<Void> result = handler.handleMissingServletRequestParameterException(
                new MissingServletRequestParameterException("id", "Long"));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("id"));
    }

    @Test
    void handleMethodArgumentTypeMismatchException_Success() {
        Result<Void> result = handler.handleMethodArgumentTypeMismatchException(
                new MethodArgumentTypeMismatchException("abc", Integer.class, "id", null, new IllegalArgumentException()));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("id"));
    }

    @Test
    void handleHttpMessageNotReadableException_Success() {
        Result<Void> result = handler.handleHttpMessageNotReadableException(new HttpMessageNotReadableException("bad body"));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), result.getCode());
    }

    @Test
    void handleHttpRequestMethodNotSupportedException_Success() {
        Result<Void> result = handler.handleHttpRequestMethodNotSupportedException(
                new HttpRequestMethodNotSupportedException("POST"));

        assertEquals(ResultCode.METHOD_NOT_ALLOWED.getCode(), result.getCode());
    }

    @Test
    void handleNoHandlerFoundException_Success() throws Exception {
        Result<Void> result = handler.handleNoHandlerFoundException(
                new NoHandlerFoundException("GET", "/missing", new HttpHeaders()));

        assertEquals(ResultCode.NOT_FOUND.getCode(), result.getCode());
    }

    @Test
    void handleMaxUploadSizeExceededException_Success() {
        Result<Void> result = handler.handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(1024));

        assertEquals(ResultCode.PARAM_ERROR.getCode(), result.getCode());
    }

    @Test
    void handleAuthenticationException_BadCredentials() {
        Result<Void> result = handler.handleAuthenticationException(new BadCredentialsException("bad credentials"));

        assertEquals(ResultCode.USER_PASSWORD_ERROR.getCode(), result.getCode());
    }

    @Test
    void handleAuthenticationException_Generic() {
        AuthenticationException exception = new AuthenticationException("unauthorized") { };

        Result<Void> result = handler.handleAuthenticationException(exception);

        assertEquals(ResultCode.UNAUTHORIZED.getCode(), result.getCode());
    }

    @Test
    void handleAccessDeniedException_Success() {
        Result<Void> result = handler.handleAccessDeniedException(new AccessDeniedException("denied"));

        assertEquals(ResultCode.FORBIDDEN.getCode(), result.getCode());
    }

    @Test
    void handleException_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/books");

        Result<Void> result = handler.handleException(new RuntimeException("boom"), request);

        assertEquals(ResultCode.ERROR.getCode(), result.getCode());
    }
}
