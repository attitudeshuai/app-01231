<template>
  <div class="page-container">
    <el-card>
      <template #header><span>我的借阅</span></template>
      <el-tabs v-model="activeTab" @tab-change="fetchData">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="借阅中" name="borrowing" />
        <el-tab-pane label="已归还" name="returned" />
      </el-tabs>
      <el-table :data="borrowList" v-loading="loading" stripe>
        <el-table-column prop="bookTitle" label="图书" min-width="200" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="borrowDate" label="借阅日期" width="120">
          <template #default="{ row }">{{ formatDate(row.borrowDate) }}</template>
        </el-table-column>
        <el-table-column prop="dueDate" label="应还日期" width="120">
          <template #default="{ row }">{{ formatDate(row.dueDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" v-if="activeTab !== 'returned'">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleReturn(row)">归还</el-button>
              <el-button type="primary" link size="small" @click="handleRenew(row)" :disabled="row.renewCount >= 1 || row.status === 2">续借</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyBorrows, returnBook, renewBook, type BorrowRecord } from '@/api/borrow'
import dayjs from 'dayjs'

const loading = ref(false)
const borrowList = ref<BorrowRecord[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const activeTab = ref('all')

const formatDate = (date: string) => dayjs(date).format('YYYY-MM-DD')
const getStatusType = (status: number) => ({ 0: 'primary', 1: 'success', 2: 'danger' }[status] as any)
const getStatusText = (status: number) => ({ 0: '借阅中', 1: '已归还', 2: '逾期' }[status])

const fetchData = async () => {
  loading.value = true
  const status = activeTab.value === 'all' ? undefined : activeTab.value === 'borrowing' ? 0 : 1
  try {
    const res = await getMyBorrows({ status, pageNum: pageNum.value, pageSize: pageSize.value })
    borrowList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleReturn = async (row: BorrowRecord) => {
  await ElMessageBox.confirm(`确定归还《${row.bookTitle}》吗？`, '归还确认')
  await returnBook(row.id)
  ElMessage.success('归还成功')
  fetchData()
}

const handleRenew = async (row: BorrowRecord) => {
  await ElMessageBox.confirm(`确定续借《${row.bookTitle}》吗？`, '续借确认')
  await renewBook(row.id)
  ElMessage.success('续借成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style lang="scss" scoped>
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  
  :deep(.el-button) {
    padding: 4px 8px;
  }
}

.pagination-container {
  margin-top: 16px;
}

@media (max-width: 768px) {
  .pagination-container {
    :deep(.el-pagination) {
      flex-wrap: wrap;
      justify-content: center;
    }
  }
}
</style>
