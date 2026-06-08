<template>
  <div class="page-container">
    <el-card>
      <template #header><span>库存预警</span></template>
      <el-table :data="bookList" v-loading="loading" stripe>
        <el-table-column prop="title" label="书名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="库存" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="danger">{{ row.availableStock }} / {{ row.totalStock }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleAdjust(row)">调整库存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="库存调整" width="400px">
      <el-form :model="stockForm" label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="stockForm.type">
            <el-radio :label="1">入库</el-radio>
            <el-radio :label="2">出库</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="stockForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStockWarning, adjustStock, type Book } from '@/api/book'

const loading = ref(false)
const bookList = ref<Book[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const currentBook = ref<Book | null>(null)

const stockForm = reactive({ type: 1, quantity: 1, remark: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStockWarning(pageNum.value, pageSize.value)
    bookList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleAdjust = (row: Book) => {
  currentBook.value = row
  stockForm.type = 1
  stockForm.quantity = 1
  stockForm.remark = ''
  dialogVisible.value = true
}

const submitAdjust = async () => {
  if (!currentBook.value) return
  try {
    await adjustStock(currentBook.value.id, stockForm)
    ElMessage.success('调整成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('调整失败:', error)
  }
}

onMounted(fetchData)
</script>
