<template>
  <div class="page-container">
    <div class="table-toolbar"><el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增角色</el-button></div>
    <el-card>
      <el-table :data="roleList" v-loading="loading" stripe>
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
                <el-button type="primary" link size="small">
                  更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="permission">
                      <el-icon><Key /></el-icon>分配权限
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="roleName"><el-input v-model="formData.roleName" /></el-form-item>
        <el-form-item label="编码" prop="roleCode"><el-input v-model="formData.roleCode" :disabled="isEdit" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="formData.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="500px">
      <el-tree ref="treeRef" :data="permissionTree" :props="{ label: 'permissionName', children: 'children' }" show-checkbox node-key="id" :default-checked-keys="checkedKeys" />
      <template #footer><el-button @click="permissionDialogVisible = false">取消</el-button><el-button type="primary" @click="handleSavePermission">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { getRoleList, createRole, updateRole, deleteRole, getRolePermissions, assignPermissions, getPermissionTree, type Role, type RoleFormData, type Permission } from '@/api/role'
import { ArrowDown, Key, Delete } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const loading = ref(false)
const roleList = ref<Role[]>([])
const permissionTree = ref<Permission[]>([])
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const checkedKeys = ref<number[]>([])
const formRef = ref<FormInstance>()
const treeRef = ref<any>()

const formData = reactive<RoleFormData>({ roleName: '', roleCode: '', description: '' })
const rules: FormRules = { roleName: [{ required: true, message: '请输入角色名称' }], roleCode: [{ required: true, message: '请输入角色编码' }] }

const formatDate = (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm:ss')

const fetchData = async () => { loading.value = true; try { const res = await getRoleList({ pageNum: 1, pageSize: 100 }); roleList.value = res.data.list } finally { loading.value = false } }
const fetchPermissions = async () => { const res = await getPermissionTree(); permissionTree.value = res.data }

const handleAdd = () => { isEdit.value = false; currentId.value = null; Object.assign(formData, { roleName: '', roleCode: '', description: '' }); dialogVisible.value = true }
const handleEdit = (row: Role) => { isEdit.value = true; currentId.value = row.id; Object.assign(formData, row); dialogVisible.value = true }
const handleDelete = async (row: Role) => { await ElMessageBox.confirm(`确定删除角色"${row.roleName}"吗？`, '提示'); await deleteRole(row.id); ElMessage.success('删除成功'); fetchData() }

const handlePermission = async (row: Role) => { currentId.value = row.id; const res = await getRolePermissions(row.id); checkedKeys.value = res.data; permissionDialogVisible.value = true }
const handleCommand = (command: string, row: Role) => {
  if (command === 'permission') handlePermission(row)
  else if (command === 'delete') handleDelete(row)
}
const handleSavePermission = async () => { if (!currentId.value) return; const keys = treeRef.value.getCheckedKeys(); await assignPermissions(currentId.value, keys); ElMessage.success('保存成功'); permissionDialogVisible.value = false }

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (isEdit.value && currentId.value) { await updateRole(currentId.value, formData); ElMessage.success('更新成功') }
  else { await createRole(formData); ElMessage.success('新增成功') }
  dialogVisible.value = false; fetchData()
}

onMounted(() => { fetchData(); fetchPermissions() })
</script>

<style lang="scss" scoped>
.page-container {
  .table-toolbar {
    margin-bottom: 16px;
  }
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  
  :deep(.el-button) {
    padding: 4px 8px;
  }
}
</style>
