package com.library.aspect;

import cn.hutool.json.JSONUtil;
import com.library.common.util.IpUtil;
import com.library.common.util.SecurityUtil;
import com.library.module.system.entity.OperationLogEntity;
import com.library.module.system.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @Pointcut("@annotation(com.library.aspect.OperationLog)")
    public void logPointcut() {
    }

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 执行方法
        Object result;
        Exception exception = null;
        try {
            result = point.proceed();
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 计算耗时
            long timeCost = System.currentTimeMillis() - startTime;
            // 异步保存日志
            saveLog(point, timeCost, exception);
        }
        
        return result;
    }

    /**
     * 保存操作日志
     */
    private void saveLog(ProceedingJoinPoint point, long timeCost, Exception exception) {
        try {
            // 获取注解信息
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            OperationLog annotation = method.getAnnotation(OperationLog.class);
            
            if (annotation == null) {
                return;
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) 
                    RequestContextHolder.getRequestAttributes();
            
            OperationLogEntity logEntity = new OperationLogEntity();
            logEntity.setOperation(annotation.value());
            logEntity.setMethod(point.getSignature().getDeclaringTypeName() + "." + 
                    point.getSignature().getName());
            logEntity.setTimeCost((int) timeCost);
            logEntity.setCreatedAt(LocalDateTime.now());

            // 获取当前用户
            try {
                Long userId = SecurityUtil.getCurrentUserId();
                String username = SecurityUtil.getCurrentUsername();
                logEntity.setUserId(userId);
                logEntity.setUsername(username);
            } catch (Exception e) {
                logEntity.setUserId(0L);
                logEntity.setUsername("anonymous");
            }

            // 获取请求参数
            String params = getParams(point);
            logEntity.setParams(params.length() > 2000 ? params.substring(0, 2000) : params);

            // 获取IP
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logEntity.setIp(IpUtil.getIpAddr(request));
            }

            // 设置状态
            if (exception != null) {
                logEntity.setStatus(0);
                String errorMsg = exception.getMessage();
                logEntity.setErrorMsg(errorMsg != null && errorMsg.length() > 500 ? 
                        errorMsg.substring(0, 500) : errorMsg);
            } else {
                logEntity.setStatus(1);
            }

            // 保存日志
            operationLogService.save(logEntity);
            
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    /**
     * 获取请求参数
     */
    private String getParams(ProceedingJoinPoint point) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = point.getArgs();
            
            if (paramNames == null || paramNames.length == 0) {
                return "{}";
            }
            
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < paramNames.length; i++) {
                Object arg = args[i];
                // 过滤文件类型参数
                if (arg instanceof MultipartFile) {
                    params.put(paramNames[i], "MultipartFile");
                } else if (arg instanceof MultipartFile[]) {
                    params.put(paramNames[i], "MultipartFile[]");
                } else {
                    params.put(paramNames[i], arg);
                }
            }
            
            return JSONUtil.toJsonStr(params);
        } catch (Exception e) {
            return "{}";
        }
    }
}
