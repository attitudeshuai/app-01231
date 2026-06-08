package com.library.module.system.service;

import com.library.module.system.vo.BackupFileVO;

import java.util.List;

/**
 * 数据库备份服务接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface BackupService {

    /**
     * 创建数据库备份
     *
     * @return 备份文件名
     */
    String createBackup();

    /**
     * 从备份文件恢复数据库
     *
     * @param filename 备份文件名
     */
    void restoreBackup(String filename);

    /**
     * 获取所有备份文件列表
     *
     * @return 备份文件列表
     */
    List<BackupFileVO> listBackups();

    /**
     * 删除备份文件
     *
     * @param filename 备份文件名
     */
    void deleteBackup(String filename);

    /**
     * 下载备份文件
     *
     * @param filename 备份文件名
     * @return 文件字节数组
     */
    byte[] downloadBackup(String filename);

    /**
     * 清理过期备份
     *
     * @param keepDays 保留天数
     * @return 删除的文件数量
     */
    int cleanOldBackups(int keepDays);
}
