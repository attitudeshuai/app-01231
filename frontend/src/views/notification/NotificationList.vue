<template>
  <div class="notification-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">消息通知</h2>
        <el-tag v-if="unreadCount > 0" type="danger" size="small" round>
          {{ unreadCount }} 条未读
        </el-tag>
      </div>
      <div class="header-right">
        <el-button
          v-if="unreadCount > 0"
          type="primary"
          plain
          @click="handleMarkAllRead"
        >
          全部标为已读
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="notification-card">
      <div v-loading="loading" class="notification-list">
        <template v-if="notifications.length > 0">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: item.isRead === 0 }"
            @click="handleRead(item)"
          >
            <div class="notification-icon" :class="getTypeClass(item.type)">
              <el-icon :size="18">
                <component :is="getTypeIcon(item.type)" />
              </el-icon>
            </div>
            <div class="notification-content">
              <div class="notification-title">
                <span>{{ item.title }}</span>
                <el-tag
                  v-if="item.isRead === 0"
                  type="danger"
                  size="small"
                  effect="dark"
                  round
                  class="unread-dot"
                >
                  未读
                </el-tag>
              </div>
              <div class="notification-body">{{ item.content }}</div>
              <div class="notification-time">
                <el-icon><Clock /></el-icon>
                {{ formatTime(item.createdAt) }}
              </div>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无通知" />
      </div>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadNotifications"
          @current-change="loadNotifications"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getNotificationList,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  type Notification
} from '@/api/notification'

const loading = ref(false)
const notifications = ref<Notification[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const unreadCount = ref(0)

const getTypeIcon = (type: number) => {
  switch (type) {
    case 1:
      return 'Bell'
    case 2:
      return 'Clock'
    default:
      return 'InfoFilled'
  }
}

const getTypeClass = (type: number) => {
  switch (type) {
    case 1:
      return 'type-system'
    case 2:
      return 'type-reminder'
    default:
      return 'type-default'
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days} 天前`
  return date.toLocaleDateString('zh-CN')
}

const loadNotifications = async () => {
  loading.value = true
  try {
    const res: any = await getNotificationList(pageNum.value, pageSize.value)
    notifications.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载通知失败:', error)
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  try {
    const res: any = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch (error) {
    console.error('获取未读数失败:', error)
  }
}

const handleRead = async (item: Notification) => {
  if (item.isRead === 0) {
    try {
      await markAsRead(item.id)
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach((n) => (n.isRead = 1))
    unreadCount.value = 0
    ElMessage.success('已全部标为已读')
  } catch (error) {
    console.error('全部标记已读失败:', error)
  }
}

onMounted(() => {
  loadNotifications()
  loadUnreadCount()
})
</script>

<style lang="scss" scoped>
.notification-page {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .page-title {
        font-size: 22px;
        font-weight: 700;
        color: #111827;
        margin: 0;
      }
    }
  }

  .notification-card {
    border-radius: 12px;
    border: 1px solid #e5e7eb;
  }

  .notification-list {
    min-height: 200px;
  }

  .notification-item {
    display: flex;
    gap: 16px;
    padding: 16px;
    border-bottom: 1px solid #f3f4f6;
    cursor: pointer;
    transition: background-color 0.2s;
    border-radius: 8px;
    margin-bottom: 4px;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background-color: #f9fafb;
    }

    &.unread {
      background-color: #f0f4ff;

      &:hover {
        background-color: #e8eeff;
      }
    }

    .notification-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.type-system {
        background: rgba(91, 95, 199, 0.1);
        color: #5b5fc7;
      }

      &.type-reminder {
        background: rgba(245, 158, 11, 0.1);
        color: #f59e0b;
      }

      &.type-default {
        background: rgba(107, 114, 128, 0.1);
        color: #6b7280;
      }
    }

    .notification-content {
      flex: 1;
      min-width: 0;

      .notification-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 600;
        color: #111827;
        margin-bottom: 6px;

        .unread-dot {
          flex-shrink: 0;
        }
      }

      .notification-body {
        font-size: 14px;
        color: #6b7280;
        line-height: 1.6;
        margin-bottom: 8px;
      }

      .notification-time {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #9ca3af;
      }
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    padding-top: 16px;
    border-top: 1px solid #f3f4f6;
  }
}

@media (max-width: 768px) {
  .notification-page {
    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .notification-item {
      padding: 12px;
      gap: 12px;
    }
  }
}
</style>
