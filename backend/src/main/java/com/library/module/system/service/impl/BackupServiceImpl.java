package com.library.module.system.service.impl;

import com.library.common.exception.BusinessException;
import com.library.module.system.service.BackupService;
import com.library.module.system.vo.BackupFileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 数据库备份服务实现
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Service
public class BackupServiceImpl implements BackupService {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${library.backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${library.backup.path:/app/backups}")
    private String backupPath;

    @Value("${library.backup.keep-days:7}")
    private int keepDays;

    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String createBackup() {
        ensureBackupDirectory();

        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        String filename = "backup_" + timestamp + ".sql";
        String filePath = backupPath + File.separator + filename;

        try {
            // 解析数据库连接信息
            DatabaseInfo dbInfo = parseDatabaseUrl(dbUrl);

            // 构建 mysqldump 命令
            List<String> command = new ArrayList<>();
            command.add("mysqldump");
            command.add("-h" + dbInfo.host);
            command.add("-P" + dbInfo.port);
            command.add("-u" + dbUsername);
            command.add("-p" + dbPassword);
            command.add("--single-transaction");
            command.add("--quick");
            command.add("--lock-tables=false");
            command.add(dbInfo.database);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // 写入文件
            try (InputStream is = process.getInputStream();
                 FileOutputStream fos = new FileOutputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // 读取错误信息
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder errorMsg = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorMsg.append(line).append("\n");
                    }
                    log.error("mysqldump 执行失败: {}", errorMsg);
                }
                // 删除不完整的备份文件
                Files.deleteIfExists(Paths.get(filePath));
                throw new BusinessException("数据库备份失败");
            }

            log.info("数据库备份成功: {}", filename);
            return filename;

        } catch (IOException | InterruptedException e) {
            log.error("数据库备份失败", e);
            throw new BusinessException("数据库备份失败: " + e.getMessage());
        }
    }

    @Override
    public void restoreBackup(String filename) {
        validateFilename(filename);

        String filePath = backupPath + File.separator + filename;
        File backupFile = new File(filePath);

        if (!backupFile.exists()) {
            throw new BusinessException("备份文件不存在: " + filename);
        }

        try {
            DatabaseInfo dbInfo = parseDatabaseUrl(dbUrl);

            // 构建 mysql 命令
            List<String> command = new ArrayList<>();
            command.add("mysql");
            command.add("-h" + dbInfo.host);
            command.add("-P" + dbInfo.port);
            command.add("-u" + dbUsername);
            command.add("-p" + dbPassword);
            command.add(dbInfo.database);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // 读取备份文件并写入 mysql 进程
            try (FileInputStream fis = new FileInputStream(backupFile);
                 OutputStream os = process.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder errorMsg = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorMsg.append(line).append("\n");
                    }
                    log.error("数据库恢复失败: {}", errorMsg);
                }
                throw new BusinessException("数据库恢复失败");
            }

            log.info("数据库恢复成功: {}", filename);

        } catch (IOException | InterruptedException e) {
            log.error("数据库恢复失败", e);
            throw new BusinessException("数据库恢复失败: " + e.getMessage());
        }
    }

    @Override
    public List<BackupFileVO> listBackups() {
        ensureBackupDirectory();

        List<BackupFileVO> backups = new ArrayList<>();
        Path dir = Paths.get(backupPath);

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.toString().endsWith(".sql"))
                    .sorted(Comparator.<Path, Instant>comparing(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant();
                        } catch (IOException e) {
                            return Instant.MIN;
                        }
                    }).reversed())
                    .forEach(p -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            long size = attrs.size();
                            LocalDateTime createdAt = LocalDateTime.ofInstant(
                                    attrs.creationTime().toInstant(), ZoneId.systemDefault());

                            backups.add(BackupFileVO.builder()
                                    .filename(p.getFileName().toString())
                                    .size(size)
                                    .sizeText(formatFileSize(size))
                                    .createdAt(createdAt.format(DISPLAY_FORMATTER))
                                    .path(p.toString())
                                    .build());
                        } catch (IOException e) {
                            log.warn("读取备份文件信息失败: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.error("列出备份文件失败", e);
        }

        return backups;
    }

    @Override
    public void deleteBackup(String filename) {
        validateFilename(filename);

        Path filePath = Paths.get(backupPath, filename);
        try {
            if (!Files.exists(filePath)) {
                throw new BusinessException("备份文件不存在: " + filename);
            }
            Files.delete(filePath);
            log.info("备份文件删除成功: {}", filename);
        } catch (IOException e) {
            log.error("删除备份文件失败: {}", filename, e);
            throw new BusinessException("删除备份文件失败");
        }
    }

    @Override
    public byte[] downloadBackup(String filename) {
        validateFilename(filename);

        Path filePath = Paths.get(backupPath, filename);
        try {
            if (!Files.exists(filePath)) {
                throw new BusinessException("备份文件不存在: " + filename);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取备份文件失败: {}", filename, e);
            throw new BusinessException("读取备份文件失败");
        }
    }

    @Override
    public int cleanOldBackups(int keepDays) {
        Path dir = Paths.get(backupPath);
        LocalDateTime threshold = LocalDateTime.now().minusDays(keepDays);
        int deletedCount = 0;

        try (Stream<Path> files = Files.list(dir)) {
            List<Path> oldFiles = files.filter(p -> {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                    LocalDateTime createdAt = LocalDateTime.ofInstant(
                            attrs.creationTime().toInstant(), ZoneId.systemDefault());
                    return p.toString().endsWith(".sql") && createdAt.isBefore(threshold);
                } catch (IOException e) {
                    return false;
                }
            }).toList();

            for (Path file : oldFiles) {
                try {
                    Files.delete(file);
                    deletedCount++;
                    log.info("清理过期备份: {}", file.getFileName());
                } catch (IOException e) {
                    log.warn("删除过期备份失败: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("清理过期备份失败", e);
        }

        return deletedCount;
    }

    /**
     * 定时备份任务
     */
    @Scheduled(cron = "${library.backup.cron:0 0 2 * * ?}")
    public void scheduledBackup() {
        if (!backupEnabled) {
            log.info("定时备份功能已禁用");
            return;
        }

        log.info("开始执行定时备份任务...");
        try {
            String filename = createBackup();
            log.info("定时备份完成: {}", filename);

            // 清理过期备份
            int cleaned = cleanOldBackups(keepDays);
            if (cleaned > 0) {
                log.info("清理了 {} 个过期备份文件", cleaned);
            }
        } catch (Exception e) {
            log.error("定时备份任务执行失败", e);
        }
    }

    private void ensureBackupDirectory() {
        Path dir = Paths.get(backupPath);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                log.info("创建备份目录: {}", backupPath);
            } catch (IOException e) {
                throw new BusinessException("无法创建备份目录: " + backupPath);
            }
        }
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }
        if (!filename.endsWith(".sql")) {
            throw new BusinessException("无效的备份文件格式");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BusinessException("无效的文件名");
        }
    }

    private DatabaseInfo parseDatabaseUrl(String url) {
        // jdbc:mysql://host:port/database?params
        DatabaseInfo info = new DatabaseInfo();
        String cleanUrl = url.replace("jdbc:mysql://", "");
        int questionMark = cleanUrl.indexOf('?');
        if (questionMark > 0) {
            cleanUrl = cleanUrl.substring(0, questionMark);
        }

        int slashIndex = cleanUrl.indexOf('/');
        String hostPort = cleanUrl.substring(0, slashIndex);
        info.database = cleanUrl.substring(slashIndex + 1);

        int colonIndex = hostPort.indexOf(':');
        if (colonIndex > 0) {
            info.host = hostPort.substring(0, colonIndex);
            info.port = hostPort.substring(colonIndex + 1);
        } else {
            info.host = hostPort;
            info.port = "3306";
        }

        return info;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    private static class DatabaseInfo {
        String host;
        String port;
        String database;
    }
}
