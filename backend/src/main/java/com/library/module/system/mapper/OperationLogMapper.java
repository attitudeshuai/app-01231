package com.library.module.system.mapper;

import com.library.common.base.BaseMapper;
import com.library.module.system.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志Mapper
 *
 * @author Library System
 * @since 1.0.0
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {

    /**
     * 分页查询日志
     */
    List<OperationLogEntity> selectPageLogs(@Param("params") Map<String, Object> params,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    /**
     * 统计日志数量
     */
    long countLogs(@Param("params") Map<String, Object> params);

    /**
     * 清理历史日志
     */
    int deleteBeforeDate(@Param("beforeDate") LocalDateTime beforeDate);
}
