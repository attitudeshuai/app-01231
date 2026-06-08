<template>
  <div class="register-container">
    <!-- 左侧品牌区域 -->
    <div class="register-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <el-icon :size="48"><Reading /></el-icon>
        </div>
        <h1 class="brand-title">图书管理系统</h1>
        <p class="brand-subtitle">Library Management System</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>免费注册使用</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>便捷图书借阅</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>个人借阅管理</span>
          </div>
        </div>
      </div>
      <div class="brand-decoration">
        <div class="decoration-circle circle-1"></div>
        <div class="decoration-circle circle-2"></div>
        <div class="decoration-circle circle-3"></div>
      </div>
    </div>
    
    <!-- 右侧注册区域 -->
    <div class="register-main">
      <div class="register-card">
        <div class="register-header">
          <h2>创建账号</h2>
          <p>注册成为新用户，开始使用图书管理系统</p>
        </div>
        
        <el-form 
          ref="formRef" 
          :model="formData" 
          :rules="rules" 
          class="register-form"
          @keyup.enter="handleRegister"
        >
          <el-form-item prop="username">
            <label class="form-label">用户名</label>
            <el-input
              v-model="formData.username"
              placeholder="请输入用户名（3-50个字符）"
              size="large"
            >
              <template #prefix>
                <el-icon class="input-icon"><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="email">
            <label class="form-label">邮箱 <span class="optional">(选填)</span></label>
            <el-input
              v-model="formData.email"
              placeholder="请输入邮箱地址"
              size="large"
            >
              <template #prefix>
                <el-icon class="input-icon"><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="phone">
            <label class="form-label">手机号</label>
            <el-input
              v-model="formData.phone"
              placeholder="请输入手机号"
              size="large"
              maxlength="11"
            >
              <template #prefix>
                <el-icon class="input-icon"><Phone /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item prop="password">
            <label class="form-label">密码</label>
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码（6-20个字符）"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <label class="form-label">确认密码</label>
            <el-input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="captchaCode">
            <label class="form-label">图形验证码</label>
            <div class="captcha-row">
              <el-input
                v-model="formData.captchaCode"
                placeholder="请输入图形验证码"
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
            <label class="form-label">短信验证码</label>
            <div class="sms-code-row">
              <el-input
                v-model="formData.smsCode"
                placeholder="请输入短信验证码"
                size="large"
                class="sms-code-input"
                maxlength="6"
              >
                <template #prefix>
                  <el-icon class="input-icon"><ChatDotRound /></el-icon>
                </template>
              </el-input>
              <button
                type="button"
                class="sms-send-btn"
                :class="{ disabled: smsCooldown > 0 || !formData.phone }"
                @click="handleSendSmsCode"
              >
                {{ smsCooldown > 0 ? `${smsCooldown}s 后重发` : '获取验证码' }}
              </button>
            </div>
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="register-btn"
              :loading="loading"
              @click="handleRegister"
            >
              <span v-if="!loading">注册</span>
              <span v-else>注册中...</span>
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="register-footer">
          <span class="login-tip">已有账号？</span>
          <router-link to="/login" class="login-link">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { User, Lock, Check, Message, Phone, ChatDotRound, Picture } from '@element-plus/icons-vue'
import { register, sendSmsCode, getCaptcha } from '@/api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)

// 短信验证码倒计时
const smsCooldown = ref(0)
let smsCooldownTimer: ReturnType<typeof setInterval> | null = null

// 图形验证码状态
const captchaImage = ref('')
const captchaKey = ref('')

const formData = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
  smsCode: ''
})

// 确认密码验证
const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== formData.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 手机号验证
const validatePhone = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [
    { required: true, validator: validatePhone, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
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

// 发送短信验证码
const handleSendSmsCode = async () => {
  if (smsCooldown.value > 0) return
  if (!formData.phone) {
    ElMessage.warning('请输入账号绑定手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    const res = await sendSmsCode({ phone: formData.phone })
    const maskedPhone = res.data?.maskedPhone || formData.phone
    const code = res.data?.code || ''
    ElMessage.success({
      message: `验证码已发送至 ${maskedPhone}，验证码：${code}`,
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

const handleRegister = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      await register({
        username: formData.username,
        password: formData.password,
        confirmPassword: formData.confirmPassword,
        email: formData.email || undefined,
        phone: formData.phone,
        captchaKey: captchaKey.value,
        captchaCode: formData.captchaCode,
        smsCode: formData.smsCode
      })
      
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error: any) {
      console.error('注册失败:', error)
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
$primary-light: rgba(91, 95, 199, 0.1);
$text-primary: #111827;
$text-secondary: #6B7280;
$text-muted: #9CA3AF;
$border-color: #E5E7EB;
$bg-page: #F9FAFB;

.register-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  background-color: #fff;
}

// 左侧品牌区域
.register-brand {
  flex: 1;
  position: relative;
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
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

// 右侧注册区域
.register-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $bg-page;
  padding: 40px;
  overflow-y: auto;
}

.register-card {
  width: 100%;
  max-width: 420px;
  background-color: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 20px;
  
  h2 {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 6px;
  }
  
  p {
    font-size: 14px;
    color: $text-secondary;
  }
}

.register-form {
  .el-form-item {
    margin-bottom: 14px;
  }
  
  :deep(.el-form-item__error) {
    padding-top: 2px;
  }
  
  .form-label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 4px;
    
    .optional {
      color: $text-muted;
      font-weight: 400;
    }
  }
  
  .input-icon {
    color: $text-muted;
  }
  
  :deep(.el-input__wrapper) {
    padding: 4px 15px;
    border-radius: 10px;
    box-shadow: 0 0 0 1px $border-color inset;
    
    &.is-focus {
      box-shadow: 0 0 0 1px $primary-color inset, 0 0 0 3px rgba($primary-color, 0.1);
    }
  }
  
  :deep(.el-input__inner) {
    height: 40px;
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
      height: 48px;
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
    height: 48px;
    padding: 0 18px;
    border: 1px solid #10B981;
    border-radius: 10px;
    background-color: rgba(16, 185, 129, 0.08);
    color: #10B981;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    cursor: pointer;
    transition: all 0.2s;
    outline: none;

    &:hover:not(:disabled) {
      background-color: rgba(16, 185, 129, 0.15);
    }

    &:active:not(:disabled) {
      background-color: rgba(16, 185, 129, 0.2);
    }

    &.disabled,
    &:disabled {
      color: $text-muted;
      border-color: $border-color;
      background-color: $bg-page;
      cursor: not-allowed;
    }
  }
  
  .register-btn {
    width: 100%;
    height: 46px;
    font-size: 15px;
    font-weight: 600;
    border-radius: 10px;
    background: #10B981;
    border-color: #10B981;
    
    &:hover {
      background: #059669;
      border-color: #059669;
    }
    
    &:active {
      background: #047857;
      border-color: #047857;
    }
  }
}

.register-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  
  .login-tip {
    color: $text-secondary;
  }
  
  .login-link {
    color: $primary-color;
    text-decoration: none;
    font-weight: 500;
    margin-left: 4px;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

// 响应式适配
@media (max-width: 900px) {
  .register-brand {
    display: none;
  }
  
  .register-main {
    padding: 24px;
  }
  
  .register-card {
    padding: 32px 24px;
  }
}
</style>
