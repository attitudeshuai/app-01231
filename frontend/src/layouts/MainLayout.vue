<template>
  <el-container class="layout-container">
    <!-- 移动端遮罩 -->
    <div v-if="isSmallScreen && !isCollapse" class="mobile-overlay" @click="isCollapse = true" />
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? (isSmallScreen ? '0px' : '72px') : '240px'" class="layout-aside" :class="{ 'mobile-hidden': isSmallScreen && isCollapse, 'mobile-visible': isSmallScreen && !isCollapse }">
      <!-- Logo区域 -->
      <div class="sidebar-header">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">
            <el-icon :size="20"><Reading /></el-icon>
          </div>
          <transition name="fade-text">
            <span v-show="!isCollapse" class="logo-text">图书管理</span>
          </transition>
        </div>
      </div>
      
      <!-- 导航菜单 -->
      <div class="sidebar-nav">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="aside-menu"
        >
          <el-menu-item index="/dashboard" v-if="isAdmin">
            <el-icon><Odometer /></el-icon>
            <template #title>仪表盘</template>
          </el-menu-item>
          
          <div class="menu-group-title" v-show="!isCollapse">图书</div>
          <el-menu-item index="/books">
            <el-icon><Reading /></el-icon>
            <template #title>图书列表</template>
          </el-menu-item>
          <el-menu-item index="/categories">
            <el-icon><FolderOpened /></el-icon>
            <template #title>分类管理</template>
          </el-menu-item>
          <el-menu-item index="/stock-warning" v-if="isAdmin">
            <el-icon><WarningFilled /></el-icon>
            <template #title>库存预警</template>
          </el-menu-item>
          
          <div class="menu-group-title" v-show="!isCollapse">借阅</div>
          <el-menu-item index="/borrows" v-if="isAdmin">
            <el-icon><Tickets /></el-icon>
            <template #title>借阅记录</template>
          </el-menu-item>
          <el-menu-item index="/my-borrows">
            <el-icon><Document /></el-icon>
            <template #title>我的借阅</template>
          </el-menu-item>
          <el-menu-item index="/overdue" v-if="isAdmin">
            <el-icon><Clock /></el-icon>
            <template #title>逾期列表</template>
          </el-menu-item>
          
          <div class="menu-group-title" v-show="!isCollapse">通知</div>
          <el-menu-item index="/notifications">
            <el-icon><Bell /></el-icon>
            <template #title>消息通知</template>
          </el-menu-item>

          <template v-if="isAdmin">
            <div class="menu-group-title" v-show="!isCollapse">管理</div>
            <el-menu-item index="/users">
              <el-icon><User /></el-icon>
              <template #title>用户管理</template>
            </el-menu-item>
            <el-menu-item index="/roles">
              <el-icon><UserFilled /></el-icon>
              <template #title>角色管理</template>
            </el-menu-item>
            <el-menu-item index="/logs">
              <el-icon><Notebook /></el-icon>
              <template #title>操作日志</template>
            </el-menu-item>
            <el-menu-item index="/config">
              <el-icon><Setting /></el-icon>
              <template #title>系统配置</template>
            </el-menu-item>
            <el-menu-item index="/backups">
              <el-icon><Files /></el-icon>
              <template #title>数据备份</template>
            </el-menu-item>
          </template>
        </el-menu>
      </div>
      
      <!-- 侧边栏底部 -->
      <div class="sidebar-footer">
        <div class="collapse-toggle" @click="isCollapse = !isCollapse">
          <el-icon :size="18">
            <component :is="isCollapse ? 'DArrowRight' : 'DArrowLeft'" />
          </el-icon>
          <span v-show="!isCollapse">收起菜单</span>
        </div>
      </div>
    </el-aside>
    
    <el-container class="layout-main">
      <!-- 头部 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon v-if="isSmallScreen" class="mobile-menu-btn" :size="22" @click="isCollapse = !isCollapse"><Fold /></el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">
              <el-icon style="vertical-align: middle;"><HomeFilled /></el-icon>
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <!-- 用户下拉菜单 -->
          <el-dropdown 
            @command="handleCommand" 
            class="user-dropdown"
            popper-class="user-dropdown-popper"
            trigger="click"
          >
            <div class="user-trigger">
              <div class="user-avatar-box">
                {{ username?.charAt(0)?.toUpperCase() }}
              </div>
              <div class="user-info-box" v-if="!isSmallScreen">
                <span class="user-name-text">{{ username }}</span>
                <span class="user-role-text">{{ isAdmin ? '管理员' : '用户' }}</span>
              </div>
              <el-icon class="trigger-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <div class="custom-dropdown-content">
                <!-- 用户信息区 -->
                <div class="user-profile-section">
                  <div class="profile-avatar">
                    {{ username?.charAt(0)?.toUpperCase() }}
                  </div>
                  <div class="profile-info">
                    <h4 class="profile-name">{{ username }}</h4>
                    <span class="profile-badge" :class="{ 'is-admin': isAdmin }">
                      {{ isAdmin ? '系统管理员' : '普通用户' }}
                    </span>
                  </div>
                </div>
                <!-- 菜单区 -->
                <div class="menu-section">
                  <div class="menu-item" @click="handleCommand('profile')">
                    <div class="menu-icon">
                      <el-icon><User /></el-icon>
                    </div>
                    <span class="menu-text">个人中心</span>
                    <el-icon class="menu-arrow"><ArrowRight /></el-icon>
                  </div>
                </div>
                <!-- 退出区 -->
                <div class="logout-section">
                  <div class="logout-btn" @click="handleCommand('logout')">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </div>
                </div>
              </div>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 内容 -->
      <el-main class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="page-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ArrowRight, Fold } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const isSmallScreen = ref(false)
const activeMenu = computed(() => route.path)
const username = computed(() => userStore.username)
const isAdmin = computed(() => userStore.isAdmin)

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'logout':
      userStore.logout()
      break
  }
}

const checkScreenSize = () => {
  const wasSmall = isSmallScreen.value
  isSmallScreen.value = window.innerWidth < 768
  if (window.innerWidth < 1024) {
    isCollapse.value = true
  }
  if (!wasSmall && isSmallScreen.value) {
    isCollapse.value = true
  }
}

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreenSize)
})
</script>

<style lang="scss" scoped>
$primary-color: #5B5FC7;
$primary-light: rgba(91, 95, 199, 0.1);
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border-color: #E5E7EB;
$border-light: #F3F4F6;
$bg-page: #F3F4F6;
$bg-sidebar: #FFFFFF;
$bg-hover: #F9FAFB;

.layout-container {
  height: 100vh;
  width: 100%;
  background-color: $bg-page;
}

// 侧边栏
.layout-aside {
  background-color: $bg-sidebar;
  border-right: 1px solid $border-light;
  transition: width 0.25s ease;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid $border-light;
  
  .logo {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    padding: 8px;
    border-radius: 10px;
    transition: background-color 0.2s;
    
    &:hover {
      background-color: $bg-hover;
    }
    
    .logo-icon {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, $primary-color, #7C3AED);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;
    }
    
    .logo-text {
      font-size: 16px;
      font-weight: 700;
      color: $text-primary;
      white-space: nowrap;
    }
  }
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px;
  
  .aside-menu {
    border-right: none;
    background-color: transparent;
    
    --el-menu-bg-color: transparent;
    --el-menu-text-color: #{$text-secondary};
    --el-menu-active-color: #{$primary-color};
    --el-menu-hover-bg-color: #{$bg-hover};
    
    :deep(.el-menu-item) {
      height: 40px;
      line-height: 40px;
      margin: 2px 0;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      
      .el-icon {
        font-size: 18px;
        margin-right: 12px;
      }
      
      &.is-active {
        background-color: $primary-light;
        color: $primary-color;
        font-weight: 600;
        
        &::before {
          display: none;
        }
      }
    }
    
    &.el-menu--collapse {
      :deep(.el-menu-item) {
        padding: 0 !important;
        justify-content: center;
        
        .el-icon {
          margin-right: 0;
        }
      }
    }
  }
  
  .menu-group-title {
    padding: 16px 12px 8px;
    font-size: 11px;
    font-weight: 600;
    color: $text-muted;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid $border-light;
  
  .collapse-toggle {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    color: $text-muted;
    font-size: 13px;
    transition: all 0.2s;
    
    &:hover {
      background-color: $bg-hover;
      color: $text-secondary;
    }
  }
}

// 主内容区
.layout-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

// 头部
.layout-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background-color: $bg-sidebar;
  border-bottom: 1px solid $border-light;
  flex-shrink: 0;
  
  .header-left {
    display: flex;
    align-items: center;
  }
  
  .header-right {
    display: flex;
    align-items: center;
  }
  
  .user-dropdown {
    .user-trigger {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 6px 12px 6px 6px;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
      
      &:hover {
        background-color: $bg-hover;
      }
      
      .user-avatar-box {
        width: 40px;
        height: 40px;
        background: linear-gradient(135deg, $primary-color 0%, #7C3AED 100%);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 16px;
        font-weight: 700;
        box-shadow: 0 2px 8px rgba(91, 95, 199, 0.25);
      }
      
      .user-info-box {
        display: flex;
        flex-direction: column;
        gap: 2px;
        
        .user-name-text {
          font-size: 14px;
          font-weight: 600;
          color: $text-primary;
          line-height: 1.2;
        }
        
        .user-role-text {
          font-size: 12px;
          color: $text-muted;
          line-height: 1.2;
        }
      }
      
      .trigger-arrow {
        color: $text-muted;
        font-size: 14px;
        transition: transform 0.2s;
      }
    }
  }
}


// 内容区
.layout-content {
  flex: 1;
  overflow-y: auto;
  background-color: $bg-page;
  padding: 20px 24px;
}

// 文字淡入淡出
.fade-text-enter-active,
.fade-text-leave-active {
  transition: opacity 0.2s ease;
}

.fade-text-enter-from,
.fade-text-leave-to {
  opacity: 0;
}

// 页面切换动画
.page-slide-enter-active,
.page-slide-leave-active {
  transition: all 0.25s ease;
}

.page-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.page-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

// 移动端遮罩
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 998;
}

// 移动端菜单按钮
.mobile-menu-btn {
  cursor: pointer;
  margin-right: 12px;
  color: $text-secondary;
  transition: color 0.2s;
  
  &:hover {
    color: $primary-color;
  }
}

// 响应式
@media (max-width: 768px) {
  .layout-aside {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 999;
    box-shadow: 4px 0 16px rgba(0, 0, 0, 0.1);
    
    &.mobile-hidden {
      transform: translateX(-100%);
      width: 0 !important;
    }
    
    &.mobile-visible {
      transform: translateX(0);
      width: 240px !important;
    }
  }
  
  .layout-header {
    padding: 0 16px;
  }
  
  .layout-content {
    padding: 16px 12px;
  }
}
</style>

<!-- 全局样式：下拉菜单（渲染在 body 下，需要全局样式） -->
<style lang="scss">
.user-dropdown-popper {
  padding: 0 !important;
  border: none !important;
  border-radius: 16px !important;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.12), 
              0 2px 10px rgba(0, 0, 0, 0.08) !important;
  overflow: hidden;
  
  .el-dropdown-menu {
    padding: 0;
    border: none;
    box-shadow: none;
  }
  
  .custom-dropdown-content {
    min-width: 260px;
    background: #fff;
    
    // 用户信息区
    .user-profile-section {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;
      background: linear-gradient(135deg, #FAFAFE 0%, #F5F3FF 100%);
      border-bottom: 1px solid #F3F4F6;
      
      .profile-avatar {
        width: 56px;
        height: 56px;
        background: linear-gradient(135deg, #5B5FC7 0%, #7C3AED 100%);
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 22px;
        font-weight: 700;
        box-shadow: 0 4px 14px rgba(91, 95, 199, 0.35);
        flex-shrink: 0;
      }
      
      .profile-info {
        display: flex;
        flex-direction: column;
        gap: 8px;
        
        .profile-name {
          font-size: 17px;
          font-weight: 700;
          color: #111827;
          margin: 0;
          line-height: 1.2;
        }
        
        .profile-badge {
          display: inline-flex;
          align-items: center;
          width: fit-content;
          padding: 5px 12px;
          border-radius: 20px;
          font-size: 12px;
          font-weight: 600;
          background: #E5E7EB;
          color: #6B7280;
          
          &.is-admin {
            background: linear-gradient(135deg, rgba(91, 95, 199, 0.12) 0%, rgba(124, 58, 237, 0.12) 100%);
            color: #5B5FC7;
          }
        }
      }
    }
    
    // 菜单区
    .menu-section {
      padding: 8px;
      
      .menu-item {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 14px 16px;
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.2s ease;
        
        &:hover {
          background: #F9FAFB;
          
          .menu-icon {
            background: rgba(91, 95, 199, 0.1);
            color: #5B5FC7;
          }
          
          .menu-arrow {
            opacity: 1;
            transform: translateX(0);
          }
        }
        
        .menu-icon {
          width: 40px;
          height: 40px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 10px;
          background: #F3F4F6;
          color: #6B7280;
          font-size: 18px;
          transition: all 0.2s ease;
          flex-shrink: 0;
        }
        
        .menu-text {
          flex: 1;
          font-size: 15px;
          font-weight: 500;
          color: #374151;
        }
        
        .menu-arrow {
          color: #9CA3AF;
          font-size: 14px;
          opacity: 0;
          transform: translateX(-4px);
          transition: all 0.2s ease;
        }
      }
    }
    
    // 退出区
    .logout-section {
      padding: 8px;
      border-top: 1px solid #F3F4F6;
      
      .logout-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        padding: 14px;
        border-radius: 12px;
        cursor: pointer;
        font-size: 14px;
        font-weight: 600;
        color: #EF4444;
        background: #FEF2F2;
        transition: all 0.2s ease;
        
        &:hover {
          background: #FEE2E2;
          color: #DC2626;
        }
        
        .el-icon {
          font-size: 18px;
        }
      }
    }
  }
}
</style>
