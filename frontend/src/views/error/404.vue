<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-illustration">
        <div class="error-number">
          <span>4</span>
          <div class="error-circle">
            <el-icon><QuestionFilled /></el-icon>
          </div>
          <span>4</span>
        </div>
      </div>
      <h2 class="error-title">页面未找到</h2>
      <p class="error-desc">抱歉，您访问的页面不存在或已被移除</p>
      <div class="error-actions">
        <el-button type="primary" size="large" @click="$router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button size="large" @click="$router.back()">
          <el-icon><Back /></el-icon>
          返回上页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { QuestionFilled, HomeFilled, Back } from '@element-plus/icons-vue'
</script>

<style lang="scss" scoped>
$primary-color: #5B5FC7;
$primary-light: rgba(91, 95, 199, 0.1);
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$bg-page: #F3F4F6;

.error-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $bg-page;
}

.error-content {
  text-align: center;
  max-width: 480px;
  padding: 40px;
}

.error-illustration {
  margin-bottom: 32px;
}

.error-number {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  
  span {
    font-size: 120px;
    font-weight: 800;
    color: $primary-color;
    line-height: 1;
    text-shadow: 4px 4px 0 rgba($primary-color, 0.1);
  }
  
  .error-circle {
    width: 100px;
    height: 100px;
    background: $primary-light;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    animation: float 3s ease-in-out infinite;
    
    .el-icon {
      font-size: 48px;
      color: $primary-color;
    }
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.error-title {
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 12px;
}

.error-desc {
  font-size: 16px;
  color: $text-secondary;
  margin-bottom: 32px;
  line-height: 1.6;
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  
  :deep(.el-button) {
    height: 48px;
    padding: 0 28px;
    border-radius: 10px;
    font-weight: 500;
    font-size: 15px;
  }
  
  :deep(.el-button--primary) {
    background-color: $primary-color;
    border-color: $primary-color;
    
    &:hover {
      background-color: #4A4EB5;
      border-color: #4A4EB5;
    }
  }
}
</style>
