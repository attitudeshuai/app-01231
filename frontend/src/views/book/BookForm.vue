<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <span>{{ isEdit ? '编辑图书' : '新增图书' }}</span>
      </template>
      
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px" style="max-width: 600px">
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="formData.isbn" placeholder="请输入ISBN" />
        </el-form-item>
        <el-form-item label="书名" prop="title">
          <el-input v-model="formData.title" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="formData.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="formData.publisher" placeholder="请输入出版社" />
        </el-form-item>
        <el-form-item label="出版日期">
          <el-date-picker v-model="formData.publishDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="formData.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-tree-select
            v-model="formData.categoryId"
            :data="categoryTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择分类"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="封面">
          <el-upload
            class="cover-uploader"
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/*"
          >
            <el-image v-if="formData.coverUrl" :src="formData.coverUrl" class="cover-image" fit="cover" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="formData.description" type="textarea" :rows="4" placeholder="请输入图书简介" />
        </el-form-item>
        <el-form-item label="库存" v-if="!isEdit">
          <el-input-number v-model="formData.totalStock" :min="0" />
        </el-form-item>
        <el-form-item label="馆藏位置">
          <el-input v-model="formData.location" placeholder="请输入馆藏位置" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { getBookById, createBook, updateBook, uploadCover, type BookFormData } from '@/api/book'
import { getCategoryTree, type Category } from '@/api/category'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const categoryTree = ref<Category[]>([])

const isEdit = computed(() => !!route.params.id)
const bookId = computed(() => Number(route.params.id))

const formData = reactive<BookFormData>({
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  publishDate: '',
  price: 0,
  categoryId: undefined as any,
  coverUrl: '',
  description: '',
  totalStock: 0,
  location: '',
  status: 1
})

const rules: FormRules = {
  isbn: [{ required: true, message: '请输入ISBN', trigger: 'blur' }],
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const fetchBook = async () => {
  if (!isEdit.value) return
  try {
    const res = await getBookById(bookId.value)
    Object.assign(formData, res.data)
  } catch (error) {
    console.error('获取图书详情失败:', error)
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

const handleUpload = async (options: UploadRequestOptions) => {
  try {
    const res = await uploadCover(options.file as File)
    formData.coverUrl = res.data
    ElMessage.success('上传成功')
  } catch (error) {
    console.error('上传失败:', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateBook(bookId.value, formData)
        ElMessage.success('更新成功')
      } else {
        await createBook(formData)
        ElMessage.success('新增成功')
      }
      router.push('/books')
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  fetchCategories()
  fetchBook()
})
</script>

<style lang="scss" scoped>
.cover-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;
    
    &:hover {
      border-color: #409eff;
    }
  }
}

.cover-image {
  width: 100px;
  height: 140px;
  display: block;
}

.cover-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 140px;
  text-align: center;
  line-height: 140px;
}

@media (max-width: 768px) {
  :deep(.el-form) {
    max-width: 100% !important;
  }
  
  :deep(.el-form-item__label) {
    width: 80px !important;
  }
}
</style>
