package com.library.module.system.service.impl;

import com.library.common.base.BaseServiceImpl;
import com.library.common.response.PageResult;
import com.library.module.system.dto.LogQueryDTO;
import com.library.module.system.entity.OperationLogEntity;
import com.library.module.system.mapper.OperationLogMapper;
import com.library.module.system.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志Service实现
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Service
public class OperationLogServiceImpl extends BaseServiceImpl<OperationLogMapper, OperationLogEntity> 
        implements OperationLogService {

    @Override
    public PageResult<OperationLogEntity> pageLogs(LogQueryDTO queryDTO) {
        Map<String, Object> params = new HashMap<>();
        if (queryDTO.getUsername() != null) {
            params.put("username", queryDTO.getUsername());
        }
        if (queryDTO.getOperation() != null) {
            params.put("operation", queryDTO.getOperation());
        }
        if (queryDTO.getStatus() != null) {
            params.put("status", queryDTO.getStatus());
        }
        if (queryDTO.getStartDate() != null) {
            params.put("startDate", queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            params.put("endDate", queryDTO.getEndDate());
        }

        int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        List<OperationLogEntity> list = baseMapper.selectPageLogs(params, offset, queryDTO.getPageSize());
        long total = baseMapper.countLogs(params);

        return PageResult.of(list, total, queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    @Override
    public int cleanOldLogs(int days) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(days);
        int count = baseMapper.deleteBeforeDate(beforeDate);
        log.info("清理{}天前的操作日志，共{}条", days, count);
        return count;
    }
}
