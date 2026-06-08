<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">借阅记录</h1>
        <p class="page-desc">查看和管理所有借阅记录</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="mini-stat" :class="{ active: queryParams.status === undefined }" @click="filterByStatus(undefined)">
        <span class="stat-label">全部</span>
        <span class="stat-value">{{ totalBorrows }}</span>
      </div>
      <div class="mini-stat borrowing" :class="{ active: queryParams.status === 0 }" @click="filterByStatus(0)">
        <span class="stat-label">借阅中</span>
        <span class="stat-value">{{ borrowingCount }}</span>
      </div>
      <div class="mini-stat returned" :class="{ active: queryParams.status === 1 }" @click="filterByStatus(1)">
        <span class="stat-label">已归还</span>
        <span class="stat-value">{{ returnedCount }}</span>
      </div>
      <div class="mini-stat overdue" :class="{ active: queryParams.status === 2 }" @click="filterByStatus(2)">
        <span class="stat-label">逾期</span>
        <span class="stat-value">{{ overdueCount }}</span>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item search-input">
          <el-input
            v-model="queryParams.username"
            placeholder="搜索用户名..."
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-item">
          <el-input
            v-model="queryParams.bookTitle"
            placeholder="搜索图书..."
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 借阅列表 -->
    <div class="table-card">
      <el-table :data="borrowList" v-loading="loading">
        <el-table-column label="借阅信息" min-width="280">
          <template #default="{ row }">
            <div class="borrow-cell">
              <div class="borrow-detail">
                <span class="book-title">{{ row.bookTitle }}</span>
                <span class="book-isbn">ISBN: {{ row.isbn }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借阅用户" width="120">
          <template #default="{ row }">
            <div class="user-badge">
              <el-avatar :size="24" class="user-avatar">{{ row.username?.charAt(0)?.toUpperCase() }}</el-avatar>
              <span class="user-name">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借阅日期" width="120">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.borrowDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="应还日期" width="120">
          <template #default="{ row }">
            <span class="date-text" :class="{ danger: isOverdue(row) }">{{ formatDate(row.dueDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="getStatusClass(row.status)">
              {{ getStatusText(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="罚款" width="100" align="right">
          <template #default="{ row }">
            <span class="fine-text" :class="{ highlight: row.fineAmount > 0 }">
              ¥{{ row.fineAmount?.toFixed(2) || '0.00' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons" v-if="row.status !== 1">
              <el-button type="primary" link size="small" @click="handleReturn(row)">归还</el-button>
              <el-button type="primary" link size="small" @click="handleRenew(row)" :disabled="row.renewCount >= 1">续借</el-button>
            </div>
            <span v-else class="completed-text">已完成</span>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBorrowList, returnBook, renewBook, getBorrowStatistics, type BorrowRecord, type BorrowQueryParams } from '@/api/borrow'
import { Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const loading = ref(false)
const borrowList = ref<BorrowRecord[]>([])
const total = ref(0)
const totalBorrows = ref(0)
const borrowingCount = ref(0)
const returnedCount = ref(0)
const overdueCount = ref(0)

const queryParams = reactive<BorrowQueryParams>({ username: '', bookTitle: '', status: undefined, pageNum: 1, pageSize: 10 })

const formatDate = (date: string) => dayjs(date).format('MM-DD')
const isOverdue = (row: BorrowRecord) => row.status === 2 || (row.status === 0 && dayjs(row.dueDate).isBefore(dayjs()))
const getStatusClass = (status: number) => ({ 0: 'borrowing', 1: 'returned', 2: 'overdue' }[status])
const getStatusText = (status: number) => ({ 0: '借阅中', 1: '已归还', 2: '逾期' }[status])

const filterByStatus = (status: number | undefined) => {
  queryParams.status = status
  queryParams.pageNum = 1
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getBorrowList(queryParams)
    borrowList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const res = await getBorrowStatistics()
    totalBorrows.value = res.data.totalBorrows || 0
    borrowingCount.value = res.data.borrowingCount || 0
    returnedCount.value = res.data.returnedCount || 0
    overdueCount.value = res.data.overdueCount || 0
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const handleSearch = () => { queryParams.pageNum = 1; fetchData() }
const handleReset = () => { queryParams.username = ''; queryParams.bookTitle = ''; queryParams.status = undefined; queryParams.pageNum = 1; fetchData() }

const handleReturn = async (row: BorrowRecord) => {
  await ElMessageBox.confirm(`确定归还《${row.bookTitle}》吗？`, '归还确认', { type: 'info' })
  await returnBook(row.id)
  ElMessage.success('归还成功')
  fetchData()
  fetchStatistics()
}

const handleRenew = async (row: BorrowRecord) => {
  await ElMessageBox.confirm(`确定续借《${row.bookTitle}》吗？`, '续借确认', { type: 'info' })
  await renewBook(row.id)
  ElMessage.success('续借成功')
  fetchData()
  fetchStatistics()
}

onMounted(() => {
  fetchData()
  fetchStatistics()
})
</script>

<style lang="scss" scoped>
$primary-color: #5B5FC7;
$primary-light: rgba(91, 95, 199, 0.1);
$success-color: #10B981;
$success-light: rgba(16, 185, 129, 0.1);
$warning-color: #F59E0B;
$warning-light: rgba(245, 158, 11, 0.1);
$danger-color: #EF4444;
$danger-light: rgba(239, 68, 68, 0.1);
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border-color: #E5E7EB;
$border-light: #F3F4F6;
$bg-white: #FFFFFF;
$bg-page: #F9FAFB;

.page-container {
}

// 页面标题
.page-header {
  margin-bottom: 24px;
  
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

// 统计行
.stat-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.mini-stat {
  flex: 1;
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    border-color: $border-color;
  }
  
  &.active {
    border-color: $primary-color;
    background: $primary-light;
    
    .stat-label, .stat-value {
      color: $primary-color;
    }
  }
  
  &.borrowing.active {
    border-color: $primary-color;
    background: $primary-light;
  }
  
  &.returned.active {
    border-color: $success-color;
    background: $success-light;
    
    .stat-label, .stat-value {
      color: $success-color;
    }
  }
  
  &.overdue.active {
    border-color: $danger-color;
    background: $danger-light;
    
    .stat-label, .stat-value {
      color: $danger-color;
    }
  }
  
  .stat-label {
    font-size: 13px;
    color: $text-secondary;
    font-weight: 500;
  }
  
  .stat-value {
    font-size: 18px;
    font-weight: 700;
    color: $text-primary;
  }
}

// 筛选区域
.filter-section {
  background: $bg-white;
  border-radius: 12px;
  border: 1px solid $border-light;
  padding: 20px;
  margin-bottom: 20px;
  
  .filter-row {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    align-items: center;
  }
  
  .filter-item {
    &.search-input {
      width: 200px;
    }
    
    :deep(.el-input__wrapper) {
      border-radius: 8px;
    }
    
    :deep(.el-input) {
      width: 200px;
    }
  }
  
  .filter-actions {
    display: flex;
    gap: 8px;
    margin-left: auto;
    
    :deep(.el-button) {
      height: 40px;
    }
  }
}

// 表格卡片
.table-card {
  background: $bg-white;
  border-radius: 12px;
  border: 1px solid $border-light;
  overflow: hidden;
  
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
    
    td.el-table__cell {
      padding: 16px 12px;
    }
  }
}

// 借阅单元格
.borrow-cell {
  .borrow-detail {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .book-title {
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;
    }
    
    .book-isbn {
      font-size: 12px;
      color: $text-muted;
      font-family: 'SF Mono', Monaco, 'Courier New', monospace;
    }
  }
}

// 用户徽章
.user-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .user-avatar {
    background: linear-gradient(135deg, $primary-color, #7C3AED);
    color: #fff;
    font-size: 11px;
    font-weight: 600;
  }
  
  .user-name {
    font-size: 13px;
    color: $text-primary;
    font-weight: 500;
  }
}

// 日期
.date-text {
  font-size: 13px;
  color: $text-secondary;
  
  &.danger {
    color: $danger-color;
    font-weight: 500;
  }
}

// 状态徽章
.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  
  &.borrowing {
    background: $primary-light;
    color: $primary-color;
  }
  
  &.returned {
    background: $success-light;
    color: $success-color;
  }
  
  &.overdue {
    background: $danger-light;
    color: $danger-color;
  }
}

// 罚款
.fine-text {
  font-size: 13px;
  color: $text-muted;
  
  &.highlight {
    color: $danger-color;
    font-weight: 600;
  }
}

// 操作按钮
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  
  :deep(.el-button) {
    padding: 4px 8px;
  }
}

.completed-text {
  font-size: 13px;
  color: $text-muted;
}

// 分页
.pagination-container {
  padding: 16px 20px;
  border-top: 1px solid $border-light;
  background: $bg-page;
}

// 响应式
@media (max-width: 768px) {
  .page-header {
    margin-bottom: 16px;
    
    .page-title {
      font-size: 18px;
    }
  }
  
  .stat-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
  
  .mini-stat {
    padding: 12px 14px;
    
    .stat-value {
      font-size: 16px;
    }
  }
  
  .filter-section {
    padding: 14px;
    
    .filter-row {
      flex-direction: column;
      
      .filter-item {
        width: 100% !important;
        
        :deep(.el-input) {
          width: 100% !important;
        }
      }
      
      .filter-actions {
        margin-left: 0;
        width: 100%;
        
        .el-button {
          flex: 1;
        }
      }
    }
  }
  
  .pagination-container {
    padding: 12px;
    
    :deep(.el-pagination) {
      flex-wrap: wrap;
      justify-content: center;
    }
  }
}
</style>
