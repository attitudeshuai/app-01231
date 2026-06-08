<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">个人资料</h1>
        <p class="page-desc">管理您的账号信息和安全设置</p>
      </div>
    </div>

    <el-row :gutter="24">
      <!-- 用户卡片 -->
      <el-col :xs="24" :lg="8">
        <div class="profile-card">
          <div class="profile-header">
            <el-avatar :size="80" class="profile-avatar">
              {{ userStore.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <h3 class="profile-name">{{ userStore.username }}</h3>
            <p class="profile-email">{{ userStore.email || '未设置邮箱' }}</p>
          </div>
          <div class="profile-roles">
            <span class="role-label">角色权限</span>
            <div class="role-tags">
              <span class="role-tag" v-for="role in userStore.roles" :key="role">{{ role }}</span>
            </div>
          </div>
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">12</span>
              <span class="stat-label">借阅中</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">56</span>
              <span class="stat-label">已归还</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">0</span>
              <span class="stat-label">逾期</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <!-- 设置区域 -->
      <el-col :xs="24" :lg="16">
        <div class="settings-card">
          <el-tabs v-model="activeTab" class="settings-tabs">
            <el-tab-pane label="基本信息" name="info">
              <div class="tab-content">
                <h4 class="section-title">个人信息</h4>
                <p class="section-desc">更新您的基本信息</p>
                <el-form :model="profileForm" label-position="top" class="profile-form">
                  <el-form-item label="用户名">
                    <el-input v-model="profileForm.username" disabled>
                      <template #suffix>
                        <el-icon class="lock-icon"><Lock /></el-icon>
                      </template>
                    </el-input>
                    <div class="form-hint">用户名不可修改</div>
                  </el-form-item>
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="邮箱">
                        <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="手机号">
                        <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item>
                    <el-button type="primary" @click="handleUpdateProfile">保存更改</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="安全设置" name="password">
              <div class="tab-content">
                <h4 class="section-title">修改密码</h4>
                <p class="section-desc">定期更换密码以保护账号安全</p>
                <el-form ref="pwdFormRef" :model="passwordForm" :rules="pwdRules" label-position="top" class="profile-form">
                  <el-form-item label="原密码" prop="oldPassword">
                    <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
                  </el-form-item>
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="新密码" prop="newPassword">
                        <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="确认密码" prop="confirmPassword">
                        <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item>
                    <el-button type="primary" @click="handleUpdatePassword">更新密码</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getProfile, updateProfile, updatePassword } from '@/api/user'
import { Lock } from '@element-plus/icons-vue'

const userStore = useUserStore()
const activeTab = ref('info')
const pwdFormRef = ref<FormInstance>()

const profileForm = reactive({ username: '', email: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [{ required: true, message: '请输入新密码' }, { min: 6, message: '密码至少6位' }],
  confirmPassword: [{ required: true, message: '请确认密码' }, { validator: validateConfirm }]
}

const fetchProfile = async () => {
  const res = await getProfile()
  Object.assign(profileForm, res.data)
}

const handleUpdateProfile = async () => {
  await updateProfile({ email: profileForm.email, phone: profileForm.phone })
  ElMessage.success('保存成功')
}

const handleUpdatePassword = async () => {
  await pwdFormRef.value?.validate()
  await updatePassword(passwordForm.oldPassword, passwordForm.newPassword)
  ElMessage.success('密码修改成功')
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

onMounted(fetchProfile)
</script>

<style lang="scss" scoped>
$primary-color: #5B5FC7;
$primary-light: rgba(91, 95, 199, 0.1);
$success-color: #10B981;
$success-light: rgba(16, 185, 129, 0.1);
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

// 用户卡片
.profile-card {
  background: $bg-white;
  border-radius: 16px;
  border: 1px solid $border-light;
  overflow: hidden;
  margin-bottom: 24px;
  
  .profile-header {
    text-align: center;
    padding: 32px 24px 24px;
    background: linear-gradient(135deg, $primary-color, #7C3AED);
    
    .profile-avatar {
      background: rgba(255, 255, 255, 0.2);
      color: #fff;
      font-size: 28px;
      font-weight: 700;
      border: 4px solid rgba(255, 255, 255, 0.3);
    }
    
    .profile-name {
      color: #fff;
      font-size: 20px;
      font-weight: 600;
      margin: 16px 0 4px;
    }
    
    .profile-email {
      color: rgba(255, 255, 255, 0.8);
      font-size: 14px;
    }
  }
  
  .profile-roles {
    padding: 20px 24px;
    border-bottom: 1px solid $border-light;
    
    .role-label {
      display: block;
      font-size: 12px;
      font-weight: 500;
      color: $text-muted;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 12px;
    }
    
    .role-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    
    .role-tag {
      display: inline-block;
      padding: 6px 14px;
      background: $primary-light;
      color: $primary-color;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 500;
    }
  }
  
  .profile-stats {
    display: flex;
    padding: 20px;
    
    .stat-item {
      flex: 1;
      text-align: center;
      
      &:not(:last-child) {
        border-right: 1px solid $border-light;
      }
      
      .stat-value {
        display: block;
        font-size: 24px;
        font-weight: 700;
        color: $text-primary;
      }
      
      .stat-label {
        display: block;
        font-size: 12px;
        color: $text-muted;
        margin-top: 4px;
      }
    }
  }
}

// 设置卡片
.settings-card {
  background: $bg-white;
  border-radius: 16px;
  border: 1px solid $border-light;
  padding: 24px;
  
  .settings-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 24px;
    }
    
    :deep(.el-tabs__nav-wrap::after) {
      height: 1px;
      background-color: $border-light;
    }
    
    :deep(.el-tabs__item) {
      font-weight: 500;
      color: $text-secondary;
      
      &.is-active {
        color: $primary-color;
      }
    }
    
    :deep(.el-tabs__active-bar) {
      background-color: $primary-color;
    }
  }
}

.tab-content {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 4px;
  }
  
  .section-desc {
    font-size: 14px;
    color: $text-muted;
    margin-bottom: 24px;
  }
}

.profile-form {
  max-width: 600px;
  
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 8px;
  }
  
  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
  
  .lock-icon {
    color: $text-muted;
  }
  
  .form-hint {
    font-size: 12px;
    color: $text-muted;
    margin-top: 4px;
  }
  
  :deep(.el-button--primary) {
    height: 40px;
    padding: 0 24px;
    margin-top: 8px;
  }
}
</style>
