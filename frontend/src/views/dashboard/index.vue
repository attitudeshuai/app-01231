<template>
  <div class="dashboard-container">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <h1 class="welcome-title">{{ getGreeting() }}，{{ username }} 👋</h1>
        <p class="welcome-subtitle">欢迎使用图书管理系统，这里是您的数据概览。</p>
      </div>
      <div class="welcome-date">
        <span class="date-text">{{ currentDate }}</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">用户总数</span>
          <div class="stat-icon users">
            <el-icon><User /></el-icon>
          </div>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ dashboardData?.userCount || 0 }}</span>
          <span class="stat-trend up">
            <el-icon><Top /></el-icon>
            12%
          </span>
        </div>
        <div class="stat-footer">较上月增长</div>
      </div>
      
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">图书总数</span>
          <div class="stat-icon books">
            <el-icon><Reading /></el-icon>
          </div>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ dashboardData?.bookCount || 0 }}</span>
          <span class="stat-trend up">
            <el-icon><Top /></el-icon>
            8%
          </span>
        </div>
        <div class="stat-footer">较上月增长</div>
      </div>
      
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">借阅中</span>
          <div class="stat-icon borrows">
            <el-icon><Tickets /></el-icon>
          </div>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ dashboardData?.borrowStats?.borrowingCount || 0 }}</span>
        </div>
        <div class="stat-footer">当前借阅数量</div>
      </div>
      
      <div class="stat-card warning">
        <div class="stat-header">
          <span class="stat-label">逾期数量</span>
          <div class="stat-icon overdue">
            <el-icon><WarningFilled /></el-icon>
          </div>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ dashboardData?.overdueCount || 0 }}</span>
        </div>
        <div class="stat-footer">需要处理</div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <h3 class="section-title">快捷操作</h3>
      <div class="actions-grid">
        <div class="action-card" @click="$router.push('/books/add')">
          <div class="action-icon add">
            <el-icon><Plus /></el-icon>
          </div>
          <div class="action-info">
            <span class="action-title">新增图书</span>
            <span class="action-desc">添加新的图书到系统</span>
          </div>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </div>
        
        <div class="action-card" @click="$router.push('/books')">
          <div class="action-icon search">
            <el-icon><Search /></el-icon>
          </div>
          <div class="action-info">
            <span class="action-title">图书查询</span>
            <span class="action-desc">搜索和浏览图书</span>
          </div>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </div>
        
        <div class="action-card" @click="$router.push('/my-borrows')">
          <div class="action-icon borrow">
            <el-icon><Document /></el-icon>
          </div>
          <div class="action-info">
            <span class="action-title">我的借阅</span>
            <span class="action-desc">查看借阅记录</span>
          </div>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </div>
        
        <div class="action-card" @click="$router.push('/categories')">
          <div class="action-icon category">
            <el-icon><FolderOpened /></el-icon>
          </div>
          <div class="action-info">
            <span class="action-title">分类管理</span>
            <span class="action-desc">管理图书分类</span>
          </div>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <el-row :gutter="24">
      <!-- 库存预警 -->
      <el-col :xs="24" :lg="12">
        <div class="data-card">
          <div class="card-header">
            <div class="header-left">
              <div class="header-icon warning">
                <el-icon><WarningFilled /></el-icon>
              </div>
              <div class="header-text">
                <h3>库存预警</h3>
                <p>库存不足的图书</p>
              </div>
            </div>
            <el-button type="primary" text size="small" @click="$router.push('/stock-warning')">
              查看全部
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div class="card-body">
            <el-table 
              :data="dashboardData?.stockWarningBooks || []" 
              size="small"
              :show-header="false"
              class="clean-table"
            >
              <el-table-column prop="title" min-width="150">
                <template #default="{ row }">
                  <div class="book-info">
                    <span class="book-title">{{ row.title }}</span>
                    <span class="book-author">{{ row.author }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column width="90" align="right">
                <template #default="{ row }">
                  <span class="stock-badge danger">{{ row.availableStock }}本</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!dashboardData?.stockWarningBooks?.length" class="empty-state">
              <el-icon><CircleCheck /></el-icon>
              <span>暂无库存预警</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 逾期列表 -->
      <el-col :xs="24" :lg="12">
        <div class="data-card">
          <div class="card-header">
            <div class="header-left">
              <div class="header-icon danger">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="header-text">
                <h3>逾期借阅</h3>
                <p>需要催还的图书</p>
              </div>
            </div>
            <el-button type="primary" text size="small" @click="$router.push('/overdue')">
              查看全部
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div class="card-body">
            <el-table 
              :data="dashboardData?.overdueRecords || []" 
              size="small"
              :show-header="false"
              class="clean-table"
            >
              <el-table-column min-width="150">
                <template #default="{ row }">
                  <div class="borrow-info">
                    <span class="borrow-book">{{ row.bookTitle }}</span>
                    <span class="borrow-user">{{ row.username }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column width="100" align="right">
                <template #default="{ row }">
                  <span class="due-date">{{ formatDate(row.dueDate) }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!dashboardData?.overdueRecords?.length" class="empty-state">
              <el-icon><CircleCheck /></el-icon>
              <span>暂无逾期记录</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getDashboardData, type DashboardData } from '@/api/system'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
const dashboardData = ref<DashboardData>()
const username = computed(() => userStore.username)

const currentDate = computed(() => {
  return dayjs().format('YYYY年MM月DD日 dddd')
})

const getGreeting = () => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

const formatDate = (date: string) => {
  return dayjs(date).format('MM-DD')
}

const fetchDashboardData = async () => {
  try {
    const res = await getDashboardData()
    dashboardData.value = res.data
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
  }
}

onMounted(() => {
  fetchDashboardData()
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

.dashboard-container {
}

// 欢迎区域
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  
  .welcome-title {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 4px;
  }
  
  .welcome-subtitle {
    color: $text-secondary;
    font-size: 14px;
  }
  
  .welcome-date {
    .date-text {
      color: $text-muted;
      font-size: 13px;
    }
  }
}

// 统计卡片
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
  
  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  background: $bg-white;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid $border-light;
  transition: all 0.2s ease;
  
  &:hover {
    border-color: $border-color;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }
  
  &.warning .stat-value {
    color: $danger-color;
  }
  
  .stat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    .stat-label {
      font-size: 13px;
      color: $text-secondary;
      font-weight: 500;
    }
    
    .stat-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      
      &.users {
        background: $primary-light;
        color: $primary-color;
      }
      
      &.books {
        background: $success-light;
        color: $success-color;
      }
      
      &.borrows {
        background: rgba(59, 130, 246, 0.1);
        color: #3B82F6;
      }
      
      &.overdue {
        background: $danger-light;
        color: $danger-color;
      }
    }
  }
  
  .stat-body {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 8px;
    
    .stat-value {
      font-size: 32px;
      font-weight: 700;
      color: $text-primary;
      line-height: 1;
    }
    
    .stat-trend {
      display: flex;
      align-items: center;
      gap: 2px;
      font-size: 12px;
      font-weight: 500;
      padding: 2px 6px;
      border-radius: 4px;
      
      &.up {
        background: $success-light;
        color: $success-color;
      }
      
      &.down {
        background: $danger-light;
        color: $danger-color;
      }
    }
  }
  
  .stat-footer {
    font-size: 12px;
    color: $text-muted;
  }
}

// 快捷操作
.quick-actions {
  margin-bottom: 32px;
  
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 16px;
  }
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  
  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    border-color: $primary-color;
    box-shadow: 0 4px 12px rgba($primary-color, 0.1);
    
    .action-arrow {
      transform: translateX(4px);
      color: $primary-color;
    }
  }
  
  .action-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
    
    &.add {
      background: $primary-light;
      color: $primary-color;
    }
    
    &.search {
      background: $success-light;
      color: $success-color;
    }
    
    &.borrow {
      background: $warning-light;
      color: $warning-color;
    }
    
    &.category {
      background: rgba(107, 114, 128, 0.1);
      color: $text-secondary;
    }
  }
  
  .action-info {
    flex: 1;
    min-width: 0;
    
    .action-title {
      display: block;
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;
      margin-bottom: 2px;
    }
    
    .action-desc {
      display: block;
      font-size: 12px;
      color: $text-muted;
    }
  }
  
  .action-arrow {
    color: $text-muted;
    font-size: 16px;
    transition: all 0.2s;
  }
}

// 数据卡片
.data-card {
  background: $bg-white;
  border-radius: 12px;
  border: 1px solid $border-light;
  margin-bottom: 24px;
  overflow: hidden;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid $border-light;
    
    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    
    .header-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      
      &.warning {
        background: $warning-light;
        color: $warning-color;
      }
      
      &.danger {
        background: $danger-light;
        color: $danger-color;
      }
    }
    
    .header-text {
      h3 {
        font-size: 15px;
        font-weight: 600;
        color: $text-primary;
        margin-bottom: 2px;
      }
      
      p {
        font-size: 12px;
        color: $text-muted;
      }
    }
    
    :deep(.el-button) {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
  
  .card-body {
    padding: 0;
    
    .clean-table {
      --el-table-border-color: transparent;
      
      :deep(.el-table__row) {
        cursor: pointer;
        
        &:hover td {
          background-color: $bg-page !important;
        }
        
        td {
          padding: 16px 20px;
          border-bottom: 1px solid $border-light;
        }
        
        &:last-child td {
          border-bottom: none;
        }
      }
    }
    
    .book-info, .borrow-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
      
      .book-title, .borrow-book {
        font-size: 14px;
        font-weight: 500;
        color: $text-primary;
      }
      
      .book-author, .borrow-user {
        font-size: 12px;
        color: $text-muted;
      }
    }
    
    .stock-badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
      white-space: nowrap;
      
      &.danger {
        background: $danger-light;
        color: $danger-color;
      }
    }
    
    .due-date {
      font-size: 13px;
      color: $danger-color;
      font-weight: 500;
    }
    
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px;
      color: $text-muted;
      
      .el-icon {
        font-size: 32px;
        margin-bottom: 8px;
        color: $success-color;
      }
      
      span {
        font-size: 13px;
      }
    }
  }
}
</style>
