<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-icon">
        <el-icon :size="80"><Lock /></el-icon>
      </div>
      <h1 class="error-code">403</h1>
      <h2 class="error-title">访问受限</h2>
      <p class="error-desc">抱歉，您没有权限访问此页面</p>
      <div class="error-actions">
        <el-button type="primary" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回上页
        </el-button>
        <el-button @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Lock, ArrowLeft, HomeFilled } from '@element-plus/icons-vue'

const router = useRouter()

const goBack = () => {
  router.go(-1)
}

const goHome = () => {
  router.push('/')
}
</script>

<style lang="scss" scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F9FAFB 0%, #F3F4F6 100%);
  padding: 24px;
}

.error-content {
  text-align: center;
  max-width: 400px;
}

.error-icon {
  margin-bottom: 24px;
  color: #EF4444;
  
  .el-icon {
    padding: 24px;
    background: #FEE2E2;
    border-radius: 50%;
  }
}

.error-code {
  font-size: 72px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 8px;
  line-height: 1;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 12px;
}

.error-desc {
  font-size: 15px;
  color: #6B7280;
  margin: 0 0 32px;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  
  .el-button {
    padding: 12px 24px;
    border-radius: 10px;
    font-weight: 500;
  }
}
</style>
