<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="用户名"><el-input v-model="queryParams.username" placeholder="用户名" clearable style="width: 150px" /></el-form-item>
        <el-form-item label="操作"><el-input v-model="queryParams.operation" placeholder="操作描述" clearable style="width: 150px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="成功" :value="1" /><el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card>
      <el-table :data="logList" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户" width="100" />
        <el-table-column prop="operation" label="操作" width="150" />
        <el-table-column prop="method" label="方法" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="timeCost" label="耗时(ms)" width="100" align="right" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getLogList, type OperationLog, type LogQueryParams } from '@/api/system'
import dayjs from 'dayjs'

const loading = ref(false)
const logList = ref<OperationLog[]>([])
const total = ref(0)
const queryParams = reactive<LogQueryParams>({ username: '', operation: '', status: undefined, pageNum: 1, pageSize: 10 })

const formatDate = (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm:ss')

const fetchData = async () => { loading.value = true; try { const res = await getLogList(queryParams); logList.value = res.data.list; total.value = res.data.total } finally { loading.value = false } }
const handleSearch = () => { queryParams.pageNum = 1; fetchData() }
const handleReset = () => { queryParams.username = ''; queryParams.operation = ''; queryParams.status = undefined; queryParams.pageNum = 1; fetchData() }

onMounted(fetchData)
</script>

<style scoped>.search-card { margin-bottom: 16px; }</style>
