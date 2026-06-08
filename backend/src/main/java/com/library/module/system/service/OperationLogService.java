package com.library.module.system.service;

import com.library.common.base.BaseService;
import com.library.common.response.PageResult;
import com.library.module.system.dto.LogQueryDTO;
import com.library.module.system.entity.OperationLogEntity;

/**
 * 操作日志Service接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface OperationLogService extends BaseService<OperationLogEntity> {

    /**
     * 分页查询日志
     */
    PageResult<OperationLogEntity> pageLogs(LogQueryDTO queryDTO);

    /**
     * 清理历史日志
     */
    int cleanOldLogs(int days);
}
