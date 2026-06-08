package com.library.module.system.controller;

import com.library.aspect.OperationLog;
import com.library.common.response.PageResult;
import com.library.common.response.Result;
import com.library.module.book.service.BookService;
import com.library.module.borrow.service.BorrowService;
import com.library.module.system.dto.LogQueryDTO;
import com.library.module.system.entity.OperationLogEntity;
import com.library.module.system.entity.SystemConfig;
import com.library.module.system.service.BackupService;
import com.library.module.system.service.OperationLogService;
import com.library.module.system.service.SystemConfigService;
import com.library.module.system.vo.BackupFileVO;
import com.library.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统Controller
 *
 * <p>提供系统管理相关的RESTful API，包括操作日志查询、系统配置管理、
 * 仪表盘数据汇总、数据备份与恢复、日志清理等功能。
 * 所有接口均需要管理员角色（类级 {@code @PreAuthorize}）。</p>
 *
 * @author Library System
 * @since 1.0.0
 */
@Tag(name = "系统管理")
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemController {

    private final OperationLogService operationLogService;
    private final SystemConfigService systemConfigService;
    private final UserService userService;
    private final BookService bookService;
    private final BorrowService borrowService;
    private final BackupService backupService;

    @Operation(summary = "查询操作日志")
    @GetMapping("/logs")
    public Result<PageResult<OperationLogEntity>> logs(LogQueryDTO queryDTO) {
        PageResult<OperationLogEntity> page = operationLogService.pageLogs(queryDTO);
        return Result.success(page);
    }

    @Operation(summary = "获取系统配置")
    @GetMapping("/config")
    public Result<List<SystemConfig>> getConfig() {
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        return Result.success(configs);
    }

    @Operation(summary = "更新系统配置")
    @PutMapping("/config")
    @OperationLog(value = "更新系统配置", type = OperationLog.OperationType.UPDATE)
    public Result<Void> updateConfig(@RequestBody Map<String, String> configs) {
        systemConfigService.batchUpdateConfig(configs);
        return Result.success();
    }

    /**
     * 获取仪表盘概览数据
     *
     * <p>汇总用户数、图书数、借阅统计、库存预警、逾期记录等数据。</p>
     *
     * @return 仪表盘数据
     */
    @Operation(summary = "获取仪表盘数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        
        // 用户统计
        data.put("userCount", userService.count(new HashMap<>()));
        
        // 图书统计
        data.put("bookCount", bookService.count(new HashMap<>()));
        
        // 借阅统计
        Map<String, Object> borrowStats = borrowService.getStatistics();
        data.put("borrowStats", borrowStats);
        
        // 库存预警
        PageResult<?> stockWarning = bookService.getStockWarning(1, 5);
        data.put("stockWarningCount", stockWarning.getTotal());
        data.put("stockWarningBooks", stockWarning.getList());
        
        // 逾期统计
        PageResult<?> overdueList = borrowService.getOverdueList(1, 5);
        data.put("overdueCount", overdueList.getTotal());
        data.put("overdueRecords", overdueList.getList());
        
        return Result.success(data);
    }

    @Operation(summary = "创建数据备份")
    @PostMapping("/backup")
    @OperationLog(value = "数据备份", type = OperationLog.OperationType.OTHER)
    public Result<String> createBackup() {
        String filename = backupService.createBackup();
        return Result.success("备份成功", filename);
    }

    @Operation(summary = "获取备份文件列表")
    @GetMapping("/backups")
    public Result<List<BackupFileVO>> listBackups() {
        List<BackupFileVO> backups = backupService.listBackups();
        return Result.success(backups);
    }

    @Operation(summary = "恢复数据备份")
    @PostMapping("/backup/restore")
    @OperationLog(value = "数据恢复", type = OperationLog.OperationType.OTHER)
    public Result<Void> restoreBackup(@RequestBody Map<String, String> request) {
        String filename = request.get("filename");
        backupService.restoreBackup(filename);
        return Result.success();
    }

    /**
     * 下载指定备份文件
     *
     * @param filename 备份文件名
     * @param response HTTP响应对象，用于写入文件流
     * @throws IOException 文件读取失败时抛出
     */
    @Operation(summary = "下载备份文件")
    @GetMapping("/backup/download/{filename}")
    public void downloadBackup(@PathVariable String filename, HttpServletResponse response) throws IOException {
        byte[] data = backupService.downloadBackup(filename);

        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedFilename);

        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    @Operation(summary = "删除备份文件")
    @DeleteMapping("/backup/{filename}")
    @OperationLog(value = "删除备份", type = OperationLog.OperationType.DELETE)
    public Result<Void> deleteBackup(@PathVariable String filename) {
        backupService.deleteBackup(filename);
        return Result.success();
    }

    @Operation(summary = "清理过期备份")
    @DeleteMapping("/backups/clean")
    @OperationLog(value = "清理过期备份", type = OperationLog.OperationType.DELETE)
    public Result<Integer> cleanBackups(@RequestParam(defaultValue = "7") int days) {
        int count = backupService.cleanOldBackups(days);
        return Result.success(count);
    }

    @Operation(summary = "清理历史日志")
    @DeleteMapping("/logs/clean")
    @OperationLog(value = "清理历史日志", type = OperationLog.OperationType.DELETE)
    public Result<Integer> cleanLogs(@RequestParam(defaultValue = "30") int days) {
        int count = operationLogService.cleanOldLogs(days);
        return Result.success(count);
    }
}
