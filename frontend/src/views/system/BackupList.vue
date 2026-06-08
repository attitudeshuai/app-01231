<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">数据备份</h1>
        <p class="page-desc">管理数据库备份文件，支持手动备份和恢复</p>
      </div>
      <div class="header-actions">
        <el-button @click="handleClean">
          <el-icon><Delete /></el-icon>
          清理过期
        </el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">
          <el-icon><Plus /></el-icon>
          创建备份
        </el-button>
      </div>
    </div>

    <!-- 备份列表 -->
    <div class="table-card">
      <el-table :data="backupList" v-loading="loading">
        <el-table-column prop="filename" label="文件名" min-width="280">
          <template #default="{ row }">
            <div class="file-cell">
              <el-icon class="file-icon"><Document /></el-icon>
              <span class="file-name">{{ row.filename }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sizeText" label="文件大小" width="120" align="right">
          <template #default="{ row }">
            <span class="size-text">{{ row.sizeText }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleDownload(row)">下载</el-button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
                <el-button type="primary" link size="small">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="restore">
                      <el-icon><RefreshRight /></el-icon>恢复
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && backupList.length === 0" description="暂无备份文件" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBackupList,
  createBackup,
  restoreBackup,
  deleteBackup,
  cleanBackups,
  getBackupDownloadUrl,
  type BackupFile
} from '@/api/backup'
import { downloadFile } from '@/utils/download'
import { Delete, RefreshRight, Document, ArrowDown } from '@element-plus/icons-vue'

const loading = ref(false)
const creating = ref(false)
const backupList = ref<BackupFile[]>([])

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getBackupList()
    backupList.value = res.data
  } catch (error) {
    console.error('获取备份列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  creating.value = true
  try {
    await createBackup()
    ElMessage.success('备份创建成功')
    fetchList()
  } catch (error) {
    console.error('创建备份失败:', error)
  } finally {
    creating.value = false
  }
}

const handleDownload = async (row: BackupFile) => {
  try {
    await downloadFile(getBackupDownloadUrl(row.filename), row.filename)
  } catch (error) {
    console.error('下载失败:', error)
  }
}

const handleRestore = async (row: BackupFile) => {
  try {
    await ElMessageBox.confirm(
      `确定要从备份文件 "${row.filename}" 恢复数据吗？这将覆盖当前数据库中的所有数据！`,
      '恢复确认',
      { type: 'warning', confirmButtonText: '确定恢复', cancelButtonText: '取消' }
    )
    
    loading.value = true
    await restoreBackup(row.filename)
    ElMessage.success('数据恢复成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('恢复失败:', error)
    }
  } finally {
    loading.value = false
  }
}

const handleDelete = async (row: BackupFile) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除备份文件 "${row.filename}" 吗？删除后无法恢复！`,
      '删除确认',
      { type: 'warning' }
    )
    
    await deleteBackup(row.filename)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleCommand = (command: string, row: BackupFile) => {
  if (command === 'restore') handleRestore(row)
  else if (command === 'delete') handleDelete(row)
}

const handleClean = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清理7天前的备份文件吗？',
      '清理确认',
      { type: 'warning' }
    )
    
    const res = await cleanBackups(7)
    ElMessage.success(`已清理 ${res.data} 个过期备份`)
    fetchList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('清理失败:', error)
    }
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
$primary-color: #5B5FC7;
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border-light: #F3F4F6;
$bg-white: #FFFFFF;
$bg-page: #F9FAFB;

.page-container {
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .header-info {
    .page-title {
      font-size: 22px;
      font-weight: 700;
      color: $text-primary;
      margin-bottom: 4px;
    }
    
    .page-desc {
      font-size: 14px;
      color: $text-secondary;
    }
  }
  
  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.table-card {
  background: $bg-white;
  border-radius: 12px;
  border: 1px solid $border-light;
  overflow: hidden;
  padding: 20px;
  
  :deep(.el-table) {
    --el-table-header-bg-color: #{$bg-page};
    --el-table-row-hover-bg-color: #{$bg-page};
    
    th.el-table__cell {
      font-weight: 600;
      color: $text-secondary;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
  }
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .file-icon {
    font-size: 24px;
    color: $primary-color;
  }
  
  .file-name {
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
    font-family: 'SF Mono', Monaco, 'Courier New', monospace;
  }
}

.size-text {
  font-size: 13px;
  color: $text-secondary;
}

.time-text {
  font-size: 13px;
  color: $text-muted;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  
  :deep(.el-button) {
    padding: 4px 8px;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 16px;
    
    .page-title {
      font-size: 18px;
    }
    
    .header-actions {
      width: 100%;
      
      .el-button {
        flex: 1;
      }
    }
  }
  
  .table-card {
    padding: 12px;
  }
}
</style>
