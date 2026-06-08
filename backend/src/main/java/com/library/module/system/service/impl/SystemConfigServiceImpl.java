package com.library.module.system.service.impl;

import com.library.common.base.BaseServiceImpl;
import com.library.module.system.entity.SystemConfig;
import com.library.module.system.mapper.SystemConfigMapper;
import com.library.module.system.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 系统配置Service实现
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends BaseServiceImpl<SystemConfigMapper, SystemConfig> 
        implements SystemConfigService {

    @Override
    public String getConfigValue(String key) {
        SystemConfig config = baseMapper.selectByKey(key);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        String value = getConfigValue(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public Integer getIntValue(String key, Integer defaultValue) {
        String value = getConfigValue(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Boolean getBooleanValue(String key, Boolean defaultValue) {
        String value = getConfigValue(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String key, String value) {
        SystemConfig config = baseMapper.selectByKey(key);
        if (config != null) {
            baseMapper.updateValue(key, value);
            log.info("更新系统配置: {} = {}", key, value);
        } else {
            // 不存在则新增
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            save(config);
            log.info("新增系统配置: {} = {}", key, value);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateConfig(Map<String, String> configs) {
        List<SystemConfig> configList = new ArrayList<>();
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(entry.getKey());
            config.setConfigValue(entry.getValue());
            configList.add(config);
        }
        
        for (SystemConfig config : configList) {
            updateConfig(config.getConfigKey(), config.getConfigValue());
        }
        log.info("批量更新系统配置，共{}项", configs.size());
    }

    @Override
    public List<SystemConfig> getAllConfigs() {
        return listAll();
    }

    @Override
    public List<SystemConfig> getConfigsByPrefix(String prefix) {
        return baseMapper.selectByKeyPrefix(prefix);
    }
}
