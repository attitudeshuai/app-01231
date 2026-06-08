<template>
  <div class="login-container">
    <!-- 左侧品牌区域 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <el-icon :size="48"><Reading /></el-icon>
        </div>
        <h1 class="brand-title">图书管理系统</h1>
        <p class="brand-subtitle">Library Management System</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>智能图书管理</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>便捷借阅流程</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>数据统计分析</span>
          </div>
        </div>
      </div>
      <div class="brand-decoration">
        <div class="decoration-circle circle-1"></div>
        <div class="decoration-circle circle-2"></div>
        <div class="decoration-circle circle-3"></div>
      </div>
    </div>
    
    <!-- 右侧登录区域 -->
    <div class="login-main">
      <div class="login-card">
        <div class="login-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账号继续访问</p>
        </div>
        
        <el-form 
          ref="formRef" 
          :model="formData" 
          :rules="rules" 
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="formData.username"
              placeholder="用户名"
              size="large"
            >
              <template #prefix>
                <el-icon class="input-icon"><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="formData.captchaCode"
                placeholder="图形验证码"
                size="large"
                maxlength="4"
                class="captcha-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Picture /></el-icon>
                </template>
              </el-input>
              <img
                :src="captchaImage"
                class="captcha-img"
                title="点击刷新验证码"
                @click="loadCaptcha"
              />
            </div>
          </el-form-item>

          <el-form-item prop="smsCode">
            <div class="sms-hint">
              <span v-if="maskedPhone">验证码已发送至 {{ maskedPhone }}</span>
              <span v-else>验证码将发送至账号绑定的手机号，请先输入用户名</span>
            </div>
            <div class="sms-code-row">
              <el-input
                v-model="formData.smsCode"
                placeholder="短信验证码"
                size="large"
                maxlength="6"
                class="sms-code-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Message /></el-icon>
                </template>
              </el-input>
              <button
                type="button"
                class="sms-send-btn"
                :class="{ disabled: smsCooldown > 0 || !formData.username }"
                @click="handleSendSmsCode"
              >
                {{ smsCooldown > 0 ? `${smsCooldown}s 后重发` : '获取验证码' }}
              </button>
            </div>
          </el-form-item>
          
          <el-form-item class="form-action">
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              <span v-if="!loading">登录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="login-footer">
          <div class="register-link-section">
            <span class="register-tip">还没有账号？</span>
            <router-link to="/register" class="register-link">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { User, Lock, Check, Message, Picture } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { sendSmsCode, getCaptcha } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

// 短信验证码倒计时
const smsCooldown = ref(0)
let smsCooldownTimer: ReturnType<typeof setInterval> | null = null

// 图形验证码状态
const captchaImage = ref('')
const captchaKey = ref('')

// 脱敏手机号（发送短信后显示）
const maskedPhone = ref('')

const formData = reactive({
  username: '',
  password: '',
  captchaCode: '',
  smsCode: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入图形验证码', trigger: 'blur' },
    { min: 4, max: 4, message: '验证码为4位', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入短信验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ]
}

// 加载图形验证码
const loadCaptcha = async () => {
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data?.key || ''
    captchaImage.value = res.data?.image || ''
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}
loadCaptcha()

// 发送短信验证码（通过用户名自动获取后台绑定的手机号）
const handleSendSmsCode = async () => {
  if (smsCooldown.value > 0) return
  if (!formData.username) {
    ElMessage.warning('请先输入用户名，系统将向账号绑定的手机发送验证码')
    return
  }

  try {
    const res = await sendSmsCode({ username: formData.username })
    maskedPhone.value = res.data?.maskedPhone || ''
    const code = res.data?.code || ''
    ElMessage.success({
      message: `验证码已发送至 ${maskedPhone.value}，验证码：${code}`,
      duration: 10000
    })

    // 开始倒计时
    smsCooldown.value = 60
    smsCooldownTimer = setInterval(() => {
      smsCooldown.value--
      if (smsCooldown.value <= 0) {
        if (smsCooldownTimer) {
          clearInterval(smsCooldownTimer)
          smsCooldownTimer = null
        }
      }
    }, 1000)
  } catch (error: any) {
    console.error('发送验证码失败:', error)
  }
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      await userStore.login({
        username: formData.username,
        password: formData.password,
        captchaKey: captchaKey.value,
        captchaCode: formData.captchaCode,
        smsCode: formData.smsCode
      })
      ElMessage.success('登录成功')
      
      const redirect = route.query.redirect as string
      router.push(redirect || '/')
    } catch (error: any) {
      console.error('登录失败:', error)
      formData.captchaCode = ''
      formData.smsCode = ''
      loadCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onUnmounted(() => {
  if (smsCooldownTimer) {
    clearInterval(smsCooldownTimer)
    smsCooldownTimer = null
  }
})
</script>

<style lang="scss" scoped>
$primary-color: #5B5FC7;
$primary-light: rgba(91, 95, 199, 0.08);
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border-color: #E5E7EB;
$bg-page: #F9FAFB;

.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  background-color: #fff;
}

// 左侧品牌区域
.login-brand {
  flex: 1;
  position: relative;
  background: linear-gradient(135deg, $primary-color 0%, #7C3AED 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  
  .brand-content {
    position: relative;
    z-index: 2;
    text-align: center;
    color: #fff;
    padding: 40px;
  }
  
  .brand-logo {
    width: 80px;
    height: 80px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 24px;
    backdrop-filter: blur(10px);
  }
  
  .brand-title {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 8px;
    letter-spacing: -0.5px;
  }
  
  .brand-subtitle {
    font-size: 14px;
    opacity: 0.8;
    margin-bottom: 48px;
  }
  
  .brand-features {
    text-align: left;
    display: inline-block;
    
    .feature-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 0;
      font-size: 15px;
      
      .el-icon {
        width: 24px;
        height: 24px;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
      }
    }
  }
  
  // 装饰元素
  .brand-decoration {
    position: absolute;
    inset: 0;
    overflow: hidden;
    
    .decoration-circle {
      position: absolute;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      
      &.circle-1 {
        width: 300px;
        height: 300px;
        top: -100px;
        right: -100px;
      }
      
      &.circle-2 {
        width: 200px;
        height: 200px;
        bottom: -50px;
        left: -50px;
      }
      
      &.circle-3 {
        width: 150px;
        height: 150px;
        bottom: 20%;
        right: 10%;
        background: rgba(255, 255, 255, 0.05);
      }
    }
  }
}

// 右侧登录区域
.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $bg-page;
  padding: 40px;
  overflow-y: auto;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background-color: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  
  h2 {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 6px;
  }
  
  p {
    font-size: 14px;
    color: $text-muted;
  }
}

.login-form {
  .el-form-item {
    margin-bottom: 18px;
  }

  .form-action {
    margin-bottom: 0;
    margin-top: 6px;
  }
  
  :deep(.el-form-item__error) {
    padding-top: 2px;
    font-size: 12px;
  }
  
  .input-icon {
    color: $text-muted;
  }
  
  :deep(.el-input__wrapper) {
    padding: 4px 15px;
    border-radius: 10px;
    box-shadow: 0 0 0 1px $border-color inset;
    transition: box-shadow 0.2s;
    
    &.is-focus {
      box-shadow: 0 0 0 1px $primary-color inset, 0 0 0 3px rgba($primary-color, 0.1);
    }
  }
  
  :deep(.el-input__inner) {
    height: 44px;
    font-size: 14px;
  }

  .captcha-row {
    display: flex;
    gap: 10px;
    width: 100%;

    .captcha-input {
      flex: 1;
      min-width: 0;
    }

    .captcha-img {
      flex-shrink: 0;
      height: 52px;
      width: 120px;
      border-radius: 10px;
      border: 1px solid $border-color;
      cursor: pointer;
      object-fit: contain;
      background-color: #f5f5f5;
      transition: opacity 0.2s;

      &:hover {
        opacity: 0.8;
      }
    }
  }

  .sms-hint {
    width: 100%;
    font-size: 12px;
    color: $text-muted;
    margin-bottom: 8px;
    line-height: 1.4;

    span {
      display: inline-block;
    }
  }

  .sms-code-row {
    display: flex;
    gap: 10px;
    width: 100%;

    .sms-code-input {
      flex: 1;
      min-width: 0;
    }
  }

  .sms-send-btn {
    flex-shrink: 0;
    height: 52px;
    padding: 0 18px;
    border: 1px solid $primary-color;
    border-radius: 10px;
    background-color: $primary-light;
    color: $primary-color;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    cursor: pointer;
    transition: all 0.2s;
    outline: none;

    &:hover:not(:disabled) {
      background-color: rgba($primary-color, 0.15);
    }

    &:active:not(:disabled) {
      background-color: rgba($primary-color, 0.2);
    }

    &.disabled,
    &:disabled {
      color: $text-muted;
      border-color: $border-color;
      background-color: $bg-page;
      cursor: not-allowed;
    }
  }
  
  .login-btn {
    width: 100%;
    height: 48px;
    font-size: 15px;
    font-weight: 600;
    border-radius: 10px;
    background: $primary-color;
    border-color: $primary-color;
    
    &:hover {
      background: #4A4EB5;
      border-color: #4A4EB5;
    }
    
    &:active {
      background: #3D40A3;
      border-color: #3D40A3;
    }
  }
}

.login-footer {
  margin-top: 28px;
  
  .register-link-section {
    text-align: center;
    margin-bottom: 16px;
    font-size: 14px;
    
    .register-tip {
      color: $text-secondary;
    }
    
    .register-link {
      color: $primary-color;
      text-decoration: none;
      font-weight: 500;
      margin-left: 4px;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
  
}

// 响应式适配
@media (max-width: 900px) {
  .login-brand {
    display: none;
  }
  
  .login-main {
    padding: 24px;
  }
  
  .login-card {
    padding: 32px 24px;
  }
}
</style>
