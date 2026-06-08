package com.library.module.system.service;

import com.library.common.base.BaseService;
import com.library.module.system.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置Service接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface SystemConfigService extends BaseService<SystemConfig> {

    /**
     * 获取配置值
     */
    String getConfigValue(String key);

    /**
     * 获取配置值（带默认值）
     */
    String getConfigValue(String key, String defaultValue);

    /**
     * 获取整数配置值
     */
    Integer getIntValue(String key, Integer defaultValue);

    /**
     * 获取布尔配置值
     */
    Boolean getBooleanValue(String key, Boolean defaultValue);

    /**
     * 更新配置
     */
    void updateConfig(String key, String value);

    /**
     * 批量更新配置
     */
    void batchUpdateConfig(Map<String, String> configs);

    /**
     * 获取所有配置
     */
    List<SystemConfig> getAllConfigs();

    /**
     * 根据前缀获取配置
     */
    List<SystemConfig> getConfigsByPrefix(String prefix);
}
