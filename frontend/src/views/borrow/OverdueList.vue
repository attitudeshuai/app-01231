<template>
  <div class="page-container">
    <el-card>
      <template #header><span>逾期列表</span></template>
      <el-table :data="borrowList" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户" width="100" />
        <el-table-column prop="bookTitle" label="图书" min-width="200" show-overflow-tooltip />
        <el-table-column prop="borrowDate" label="借阅日期" width="120">
          <template #default="{ row }">{{ formatDate(row.borrowDate) }}</template>
        </el-table-column>
        <el-table-column prop="dueDate" label="应还日期" width="120">
          <template #default="{ row }">{{ formatDate(row.dueDate) }}</template>
        </el-table-column>
        <el-table-column label="逾期天数" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="danger">{{ getOverdueDays(row.dueDate) }}天</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预计罚款" width="100" align="right">
          <template #default="{ row }">¥{{ (getOverdueDays(row.dueDate) * 0.5).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleReturn(row)">归还</el-button>
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
import { getOverdueList, returnBook, type BorrowRecord } from '@/api/borrow'
import dayjs from 'dayjs'

const loading = ref(false)
const borrowList = ref<BorrowRecord[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const formatDate = (date: string) => dayjs(date).format('YYYY-MM-DD')
const getOverdueDays = (dueDate: string) => Math.max(0, dayjs().diff(dayjs(dueDate), 'day'))

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getOverdueList(pageNum.value, pageSize.value)
    borrowList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleReturn = async (row: BorrowRecord) => {
  await ElMessageBox.confirm(`确定归还《${row.bookTitle}》吗？逾期罚款将自动计算。`, '归还确认')
  await returnBook(row.id)
  ElMessage.success('归还成功')
  fetchData()
}

onMounted(fetchData)
</script>
