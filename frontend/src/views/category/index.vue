<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">分类管理</h1>
        <p class="page-desc">管理图书的分类层级结构</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增分类
        </el-button>
      </div>
    </div>

    <!-- 分类树 -->
    <div class="table-card">
      <el-table
        :data="categoryTree"
        row-key="id"
        v-loading="loading"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="分类名称" min-width="240">
          <template #default="{ row }">
            <div class="category-cell">
              <div class="category-icon">
                <el-icon><FolderOpened /></el-icon>
              </div>
              <span class="category-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="desc-text">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" align="center">
          <template #default="{ row }">
            <span class="sort-badge">{{ row.sortOrder }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分类表单对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="500px" class="category-dialog">
      <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="父级分类">
          <el-tree-select
            v-model="formData.parentId"
            :data="categoryTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="选择父级分类（可选）"
            clearable
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入分类描述" />
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
import { getCategoryTree, createCategory, updateCategory, deleteCategory, type Category, type CategoryFormData } from '@/api/category'

const loading = ref(false)
const categoryTree = ref<Category[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const formData = reactive<CategoryFormData>({ name: '', description: '', parentId: undefined, sortOrder: 0 })
const rules: FormRules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCategoryTree()
    categoryTree.value = res.data
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  currentId.value = null
  formData.name = ''
  formData.description = ''
  formData.parentId = undefined
  formData.sortOrder = 0
  dialogVisible.value = true
}

const handleEdit = (row: Category) => {
  isEdit.value = true
  currentId.value = row.id
  formData.name = row.name
  formData.description = row.description
  formData.parentId = row.parentId || undefined
  formData.sortOrder = row.sortOrder
  dialogVisible.value = true
}

const handleDelete = async (row: Category) => {
  await ElMessageBox.confirm(`确定删除分类"${row.name}"吗？`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (isEdit.value && currentId.value) {
    await updateCategory(currentId.value, formData)
    ElMessage.success('更新成功')
  } else {
    await createCategory(formData)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchData()
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
$primary-color: #5B5FC7;
$primary-light: rgba(91, 95, 199, 0.1);
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
      padding: 14px 12px;
    }
    
    .el-table__expand-icon {
      color: $text-muted;
    }
  }
}

// 分类单元格
.category-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .category-icon {
    width: 32px;
    height: 32px;
    background: $primary-light;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $primary-color;
    font-size: 16px;
  }
  
  .category-name {
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
  }
}

// 描述文本
.desc-text {
  font-size: 13px;
  color: $text-secondary;
}

// 排序徽章
.sort-badge {
  display: inline-block;
  min-width: 24px;
  padding: 2px 8px;
  background: $bg-page;
  border-radius: 4px;
  font-size: 12px;
  color: $text-secondary;
  font-weight: 500;
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

// 对话框
.category-dialog {
  :deep(.el-dialog__body) {
    padding: 24px;
  }
  
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 8px;
  }
  
  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    border-radius: 8px;
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>
