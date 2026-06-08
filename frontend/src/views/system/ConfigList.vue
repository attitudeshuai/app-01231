<template>
  <div class="page-container">
    <el-card>
      <template #header><span>系统配置</span></template>
      <el-form :model="configForm" label-width="150px" style="max-width: 600px">
        <el-form-item label="默认借阅天数">
          <el-input-number v-model="configForm.default_borrow_days" :min="1" :max="90" />
        </el-form-item>
        <el-form-item label="最大续借次数">
          <el-input-number v-model="configForm.max_renew_times" :min="0" :max="5" />
        </el-form-item>
        <el-form-item label="续借天数">
          <el-input-number v-model="configForm.renew_days" :min="1" :max="60" />
        </el-form-item>
        <el-form-item label="逾期罚款(元/天)">
          <el-input-number v-model="configForm.overdue_fine_per_day" :min="0" :precision="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="到期提醒天数">
          <el-input-number v-model="configForm.reminder_days_before" :min="1" :max="7" />
        </el-form-item>
        <el-form-item label="库存预警阈值">
          <el-input-number v-model="configForm.stock_warning_threshold" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="最大借阅数量">
          <el-input-number v-model="configForm.max_borrow_count" :min="1" :max="10" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, updateSystemConfig, type SystemConfig } from '@/api/system'

const saving = ref(false)
const configForm = reactive({
  default_borrow_days: 30,
  max_renew_times: 1,
  renew_days: 30,
  overdue_fine_per_day: 0.5,
  reminder_days_before: 3,
  stock_warning_threshold: 5,
  max_borrow_count: 5
})

const fetchConfig = async () => {
  try {
    const res = await getSystemConfig()
    res.data.forEach((config: SystemConfig) => {
      if (config.configKey in configForm) {
        (configForm as any)[config.configKey] = Number(config.configValue)
      }
    })
  } catch (error) { console.error('获取配置失败:', error) }
}

const handleSave = async () => {
  saving.value = true
  try {
    const configs: Record<string, string> = {}
    Object.entries(configForm).forEach(([key, value]) => { configs[key] = String(value) })
    await updateSystemConfig(configs)
    ElMessage.success('保存成功')
  } finally { saving.value = false }
}

onMounted(fetchConfig)
</script>
