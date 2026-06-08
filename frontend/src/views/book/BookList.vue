<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">图书列表</h1>
        <p class="page-desc">管理和浏览所有图书信息</p>
      </div>
      <div class="header-actions" v-if="userStore.isAdmin">
        <el-button @click="handleDownloadTemplate">
          <el-icon><Download /></el-icon>
          导入模板
        </el-button>
        <el-button @click="importDialogVisible = true">
          <el-icon><Upload /></el-icon>
          导入
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button type="primary" @click="$router.push('/books/add')">
          <el-icon><Plus /></el-icon>
          新增图书
        </el-button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-item search-input">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索书名、作者或 ISBN..."
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-item">
          <el-tree-select
            v-model="queryParams.categoryId"
            :data="categoryTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="选择分类"
            clearable
            check-strictly
          />
        </div>
        <div class="filter-item">
          <el-select v-model="queryParams.status" placeholder="状态" clearable>
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
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
        <span class="result-count">共 {{ total }} 本图书</span>
      </div>
    </div>

    <!-- 图书列表 -->
    <div class="table-card">
      <el-table :data="bookList" v-loading="loading">
        <el-table-column label="图书信息" min-width="280">
          <template #default="{ row }">
            <div class="book-cell">
              <div class="book-cover">
                <el-image
                  :src="row.coverUrl || '/placeholder-book.png'"
                  fit="cover"
                >
                  <template #error>
                    <div class="cover-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
              <div class="book-detail">
                <span class="book-title">{{ row.title }}</span>
                <span class="book-author">{{ row.author }}</span>
                <span class="book-isbn">ISBN: {{ row.isbn }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" min-width="120" align="center">
          <template #default="{ row }">
            <span class="category-tag">{{ row.categoryName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="120" align="right">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.price?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="120" align="center">
          <template #default="{ row }">
            <div class="stock-info" :class="{ warning: row.availableStock <= 5 }">
              <span class="stock-available">{{ row.availableStock }}</span>
              <span class="stock-divider">/</span>
              <span class="stock-total">{{ row.totalStock }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'active' : 'inactive'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="primary"
                link
                size="small"
                @click="handleBorrow(row)"
                :disabled="row.availableStock <= 0"
              >
                借阅
              </el-button>
              <template v-if="userStore.isAdmin">
                <el-dropdown trigger="click" @command="(cmd: string) => handleBookCommand(cmd, row)">
                  <el-button type="primary" link size="small">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="edit">
                        <el-icon><Edit /></el-icon>编辑
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <el-icon><Delete /></el-icon>删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchBookList"
          @current-change="fetchBookList"
        />
      </div>
    </div>

    <!-- 导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="批量导入图书"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="import-dialog">
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将 Excel 文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              仅支持 .xlsx、.xls 格式，单次最多导入 1000 条数据
            </div>
          </template>
        </el-upload>

        <!-- 导入结果 -->
        <div v-if="importResult" class="import-result">
          <div class="result-summary">
            <div class="result-item success">
              <span class="label">成功</span>
              <span class="value">{{ importResult.successCount }}</span>
            </div>
            <div class="result-item fail">
              <span class="label">失败</span>
              <span class="value">{{ importResult.failCount }}</span>
            </div>
            <div class="result-item total">
              <span class="label">总计</span>
              <span class="value">{{ importResult.totalCount }}</span>
            </div>
          </div>
          <div v-if="importResult.errorMessages?.length" class="error-list">
            <div class="error-title">错误信息：</div>
            <div class="error-item" v-for="(msg, index) in importResult.errorMessages" :key="index">
              {{ msg }}
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeImportDialog">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImport">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type UploadInstance, type UploadFile, type UploadRawFile } from 'element-plus'
import {
  getBookList,
  deleteBook,
  importBooksExcel,
  getImportTemplateUrl,
  getExportExcelUrl,
  type Book,
  type BookQueryParams,
  type ImportResult
} from '@/api/book'
import { getCategoryTree, type Category } from '@/api/category'
import { borrowBook } from '@/api/borrow'
import { useUserStore } from '@/stores/user'
import { downloadFile } from '@/utils/download'
import { Search, Upload, Download, UploadFilled, ArrowDown, Edit, Delete } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const bookList = ref<Book[]>([])
const total = ref(0)
const categoryTree = ref<Category[]>([])

// 导入相关
const importDialogVisible = ref(false)
const importLoading = ref(false)
const importResult = ref<ImportResult | null>(null)
const uploadRef = ref<UploadInstance>()
const importFile = ref<File | null>(null)

const queryParams = reactive<BookQueryParams>({
  keyword: '',
  categoryId: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const fetchBookList = async () => {
  loading.value = true
  try {
    const res = await getBookList(queryParams)
    bookList.value = res.data.list
    total.value = res.data.total
  } catch (error) {
    console.error('获取图书列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await getCategoryTree()
    categoryTree.value = res.data
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchBookList()
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.categoryId = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchBookList()
}

const handleBorrow = async (row: Book) => {
  try {
    await ElMessageBox.confirm(
      `确定要借阅《${row.title}》吗？`,
      '借阅确认',
      { type: 'info' }
    )
    await borrowBook(row.id)
    ElMessage.success('借阅成功')
    fetchBookList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('借阅失败:', error)
    }
  }
}

const handleDelete = async (row: Book) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除《${row.title}》吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteBook(row.id)
    ElMessage.success('删除成功')
    fetchBookList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleBookCommand = (command: string, row: Book) => {
  if (command === 'edit') {
    router.push(`/books/edit/${row.id}`)
  } else if (command === 'delete') {
    handleDelete(row)
  }
}

// 下载导入模板
const handleDownloadTemplate = async () => {
  try {
    await downloadFile(getImportTemplateUrl(), '图书导入模板.xlsx')
  } catch (error) {
    console.error('下载模板失败:', error)
  }
}

// 导出图书
const handleExport = async () => {
  try {
    await downloadFile(getExportExcelUrl(queryParams), '图书列表.xlsx')
  } catch (error) {
    console.error('导出失败:', error)
  }
}

// 处理文件选择
const handleFileChange = (file: UploadFile) => {
  importFile.value = file.raw as File
  importResult.value = null
}

// 处理超出限制
const handleExceed = (files: File[]) => {
  uploadRef.value?.clearFiles()
  const file = files[0] as UploadRawFile
  uploadRef.value?.handleStart(file)
}

// 开始导入
const handleImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  importLoading.value = true
  try {
    const res = await importBooksExcel(importFile.value)
    importResult.value = res.data
    if (res.data.successCount > 0) {
      ElMessage.success(`成功导入 ${res.data.successCount} 条数据`)
      fetchBookList()
    }
    if (res.data.failCount > 0) {
      ElMessage.warning(`${res.data.failCount} 条数据导入失败`)
    }
  } catch (error) {
    console.error('导入失败:', error)
  } finally {
    importLoading.value = false
  }
}

// 关闭导入对话框
const closeImportDialog = () => {
  importDialogVisible.value = false
  importFile.value = null
  importResult.value = null
  uploadRef.value?.clearFiles()
}

onMounted(() => {
  fetchBookList()
  fetchCategories()
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
      min-width: 240px;
      max-width: 360px;
    }
    
    :deep(.el-input__wrapper),
    :deep(.el-select__wrapper) {
      border-radius: 8px;
    }
    
    :deep(.el-select) {
      width: 140px;
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
      vertical-align: middle;
    }
  }
}

// 图书单元格
.book-cell {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  
  .book-cover {
    width: 50px;
    height: 70px;
    border-radius: 6px;
    overflow: hidden;
    flex-shrink: 0;
    background: $bg-page;
    
    :deep(.el-image) {
      width: 100%;
      height: 100%;
    }
    
    .cover-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: $text-muted;
      font-size: 20px;
    }
  }
  
  .book-detail {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
    
    .book-title {
      font-size: 14px;
      font-weight: 600;
      color: $text-primary;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    
    .book-author {
      font-size: 13px;
      color: $text-secondary;
    }
    
    .book-isbn {
      font-size: 12px;
      color: $text-muted;
      font-family: 'SF Mono', Monaco, 'Courier New', monospace;
    }
  }
}

// 分类标签
.category-tag {
  display: inline-block;
  padding: 4px 10px;
  background: $bg-page;
  border-radius: 6px;
  font-size: 12px;
  color: $text-secondary;
  font-weight: 500;
  white-space: nowrap;
}

// 价格
.price-text {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  white-space: nowrap;
}

// 库存信息
.stock-info {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  white-space: nowrap;
  
  .stock-available {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
  }
  
  .stock-divider {
    color: $text-muted;
    font-size: 12px;
  }
  
  .stock-total {
    font-size: 12px;
    color: $text-muted;
  }
  
  &.warning {
    .stock-available {
      color: $danger-color;
    }
  }
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
}

// 分页
.pagination-container {
  padding: 16px 20px;
  border-top: 1px solid $border-light;
  background: $bg-page;
}

// 导入对话框
.import-dialog {
  .import-result {
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid $border-light;
    
    .result-summary {
      display: flex;
      gap: 20px;
      margin-bottom: 16px;
      
      .result-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 12px 20px;
        border-radius: 8px;
        background: $bg-page;
        
        .label {
          font-size: 12px;
          color: $text-muted;
          margin-bottom: 4px;
        }
        
        .value {
          font-size: 20px;
          font-weight: 700;
        }
        
        &.success .value {
          color: $success-color;
        }
        
        &.fail .value {
          color: $danger-color;
        }
        
        &.total .value {
          color: $text-primary;
        }
      }
    }
    
    .error-list {
      background: $danger-light;
      border-radius: 8px;
      padding: 12px;
      max-height: 200px;
      overflow-y: auto;
      
      .error-title {
        font-size: 13px;
        font-weight: 600;
        color: $danger-color;
        margin-bottom: 8px;
      }
      
      .error-item {
        font-size: 12px;
        color: $danger-color;
        padding: 4px 0;
        
        &:not(:last-child) {
          border-bottom: 1px dashed rgba($danger-color, 0.2);
        }
      }
    }
  }
}
</style>
