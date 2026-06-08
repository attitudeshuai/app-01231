package com.library.module.system.mapper;

import com.library.common.base.BaseMapper;
import com.library.module.system.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置Mapper
 *
 * @author Library System
 * @since 1.0.0
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询
     */
    SystemConfig selectByKey(@Param("configKey") String configKey);

    /**
     * 根据配置键前缀查询
     */
    List<SystemConfig> selectByKeyPrefix(@Param("prefix") String prefix);

    /**
     * 更新配置值
     */
    int updateValue(@Param("configKey") String configKey, @Param("configValue") String configValue);

    /**
     * 批量更新配置
     */
    int batchUpdateValues(@Param("list") List<SystemConfig> configs);
}
