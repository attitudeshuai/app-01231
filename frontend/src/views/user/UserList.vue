<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">用户管理</h1>
        <p class="page-desc">管理系统中的所有用户账号</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
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
          <el-select v-model="queryParams.status" placeholder="状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
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
      <div class="filter-info">
        <span class="result-count">共 {{ total }} 个用户</span>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="table-card">
      <el-table :data="userList" v-loading="loading">
        <el-table-column label="用户信息" min-width="250">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="40" class="user-avatar">
                {{ row.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <div class="user-detail">
                <span class="user-name">{{ row.username }}</span>
                <span class="user-email">{{ row.email || '未设置邮箱' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">
            <span class="phone-text">{{ row.phone || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'active' : 'inactive'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
                <el-button type="primary" link size="small">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="resetPwd">
                      <el-icon><Key /></el-icon>重置密码
                    </el-dropdown-item>
                    <el-dropdown-item :command="row.status === 1 ? 'disable' : 'enable'" :divided="true">
                      <el-icon><CircleClose v-if="row.status === 1" /><CircleCheck v-else /></el-icon>
                      {{ row.status === 1 ? '禁用账号' : '启用账号' }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
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

    <!-- 用户表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="500px"
      class="user-dialog"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="formData.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="formData.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="角色">
          <el-select v-model="formData.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleList" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">
              <span class="radio-label">启用</span>
            </el-radio>
            <el-radio :label="0">
              <span class="radio-label">禁用</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { getUserList, createUser, updateUser, updateUserStatus, resetPassword, type User, type UserFormData, type UserQueryParams } from '@/api/user'
import { getAllRoles, type Role } from '@/api/role'
import { Search, ArrowDown, Key } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const loading = ref(false)
const userList = ref<User[]>([])
const roleList = ref<Role[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const queryParams = reactive<UserQueryParams>({ username: '', status: undefined, pageNum: 1, pageSize: 10 })
const formData = reactive<UserFormData>({ username: '', password: '', email: '', phone: '', status: 1, roleIds: [] })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const formatDate = (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserList(queryParams)
    userList.value = res.data.list
    total.value = res.data.total
  } finally { loading.value = false }
}

const fetchRoles = async () => { const res = await getAllRoles(); roleList.value = res.data }
const handleSearch = () => { queryParams.pageNum = 1; fetchData() }
const handleReset = () => { queryParams.username = ''; queryParams.status = undefined; queryParams.pageNum = 1; fetchData() }

const handleAdd = () => { isEdit.value = false; currentId.value = null; Object.assign(formData, { username: '', password: '', email: '', phone: '', status: 1, roleIds: [] }); dialogVisible.value = true }
const handleEdit = (row: User) => { isEdit.value = true; currentId.value = row.id; Object.assign(formData, { username: row.username, email: row.email, phone: row.phone, status: row.status, roleIds: [] }); dialogVisible.value = true }
const handleResetPwd = async (row: User) => { await ElMessageBox.confirm(`确定重置用户"${row.username}"的密码吗？重置后的密码将通过短信发送给用户。`, '提示'); await resetPassword(row.id); ElMessage.success('密码重置成功，新密码已通过短信通知用户') }
const handleToggleStatus = async (row: User) => { await updateUserStatus(row.id, row.status === 1 ? 0 : 1); ElMessage.success('操作成功'); fetchData() }
const handleCommand = (command: string, row: User) => {
  if (command === 'resetPwd') handleResetPwd(row)
  else if (command === 'disable' || command === 'enable') handleToggleStatus(row)
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (isEdit.value && currentId.value) { await updateUser(currentId.value, formData); ElMessage.success('更新成功') }
  else { await createUser(formData); ElMessage.success('新增成功') }
  dialogVisible.value = false; fetchData()
}

onMounted(() => { fetchData(); fetchRoles() })
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
    :deep(.el-button) {
      height: 40px;
      padding: 0 20px;
      font-weight: 500;
    }
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
      flex: 1;
      min-width: 200px;
      max-width: 300px;
    }
    
    :deep(.el-input__wrapper),
    :deep(.el-select__wrapper) {
      border-radius: 8px;
    }
    
    :deep(.el-select) {
      width: 120px;
    }
  }
  
  .filter-actions {
    display: flex;
    gap: 8px;
    
    :deep(.el-button) {
      height: 40px;
    }
  }
  
  .filter-info {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid $border-light;
    
    .result-count {
      font-size: 13px;
      color: $text-muted;
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

// 用户单元格
.user-cell {
  display: flex;
  gap: 12px;
  align-items: center;
  
  .user-avatar {
    background: linear-gradient(135deg, $primary-color, #7C3AED);
    color: #fff;
    font-weight: 600;
    flex-shrink: 0;
  }
  
  .user-detail {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    
    .user-name {
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;
    }
    
    .user-email {
      font-size: 12px;
      color: $text-muted;
    }
  }
}

// 手机号
.phone-text {
  font-family: 'SF Mono', Monaco, 'Courier New', monospace;
  font-size: 13px;
  color: $text-secondary;
}

// 日期
.date-text {
  font-size: 13px;
  color: $text-secondary;
}

// 状态徽章
.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;

  &.active {
    background: $success-light;
    color: $success-color;
  }
  
  &.inactive {
    background: $bg-page;
    color: $text-muted;
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
  
  :deep(.el-dropdown) {
    .el-button {
      padding: 4px 8px;
    }
  }
}

// 分页
.pagination-container {
  padding: 16px 20px;
  border-top: 1px solid $border-light;
  background: $bg-page;
}

// 对话框
.user-dialog {
  :deep(.el-dialog__body) {
    padding: 24px;
  }
  
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 8px;
  }
  
  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
  
  .radio-label {
    font-size: 14px;
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>
