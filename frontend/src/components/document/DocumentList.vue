<template>
  <div class="document-list">
    <div class="document-actions">
      <div class="left-actions">
        <el-button 
          type="primary" 
          :disabled="selectedDocuments.length === 0"
          @click="handleBatchDownload"
        >
          <el-icon><Download /></el-icon> 批量下载
        </el-button>
        <el-button 
          type="danger" 
          :disabled="selectedDocuments.length === 0"
          @click="confirmBatchDelete"
        >
          <el-icon><Delete /></el-icon> 批量删除
        </el-button>
      </div>
      <div class="right-actions">
        <el-select 
          v-model="categoryFilter" 
          placeholder="分类筛选" 
          clearable
          @change="handleFilterChange"
        >
          <el-option 
            v-for="category in categoryOptions" 
            :key="category.value" 
            :label="category.label" 
            :value="category.value" 
          />
        </el-select>
        <el-input
          v-model="searchQuery"
          placeholder="搜索文件名"
          clearable
          @input="handleFilterChange"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </div>
    
    <el-table
      v-loading="loading"
      :data="filteredDocuments"
      style="width: 100%"
      border
      @selection-change="handleSelectionChange"
      row-key="id"
    >
      <el-table-column type="selection" width="55" />
      
      <el-table-column prop="fileName" label="文件名" min-width="200">
        <template #default="{ row }">
          <div class="file-name-cell">
            <el-icon class="file-icon"><Document /></el-icon>
            <span class="file-name">{{ row.fileName }}</span>
          </div>
        </template>
      </el-table-column>
      
      <el-table-column prop="fileType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ getFileTypeLabel(row.fileType) }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="fileSize" label="大小" width="100">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      
      <el-table-column prop="category" label="分类" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.category" type="success" size="small">
            {{ row.category }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      
      <el-table-column prop="uploadTime" label="上传时间" width="170">
        <template #default="{ row }">
          {{ formatDate(row.uploadTime) }}
        </template>
      </el-table-column>
      
      <el-table-column prop="remarks" label="备注" min-width="150">
        <template #default="{ row }">
          <div class="remarks-cell">
            <span v-if="row.remarks">{{ row.remarks }}</span>
            <span v-else class="no-remarks">无备注</span>
            <el-button 
              type="primary" 
              size="small" 
              link 
              @click="openRemarksDialog(row)"
            >
              <el-icon><Edit /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button 
            type="primary" 
            size="small" 
            @click="handlePreview(row)"
          >
            预览
          </el-button>
          <el-button 
            type="success" 
            size="small" 
            @click="handleDownload(row)"
          >
            下载
          </el-button>
          <el-dropdown>
            <el-button size="small">
              更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openCategoryDialog(row)">
                  <el-icon><Folder /></el-icon> 更改分类
                </el-dropdown-item>
                <el-dropdown-item @click="openRemarksDialog(row)">
                  <el-icon><Edit /></el-icon> 编辑备注
                </el-dropdown-item>
                <el-dropdown-item @click="confirmDelete(row)">
                  <el-icon><Delete /></el-icon> 删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分类编辑对话框 -->
    <el-dialog
      v-model="categoryDialogVisible"
      title="更改分类"
      width="400px"
    >
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="分类">
          <el-select 
            v-model="categoryForm.category" 
            placeholder="选择分类"
            allow-create
            filterable
            default-first-option
          >
            <el-option 
              v-for="category in categoryOptions" 
              :key="category.value" 
              :label="category.label" 
              :value="category.value" 
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="categoryDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateCategory">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 备注编辑对话框 -->
    <el-dialog
      v-model="remarksDialogVisible"
      title="编辑备注"
      width="500px"
    >
      <el-form :model="remarksForm" label-width="80px">
        <el-form-item label="备注">
          <el-input 
            v-model="remarksForm.remarks" 
            type="textarea" 
            :rows="5"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="remarksDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateRemarks">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="文件预览"
      width="80%"
      top="5vh"
    >
      <div v-loading="previewLoading" class="preview-container">
        <iframe 
          v-if="previewUrl && canPreviewInBrowser(currentPreviewFile)"
          :src="previewUrl" 
          width="100%" 
          height="600px"
          frameborder="0"
        ></iframe>
        <div v-else class="preview-error">
          <el-icon><DocumentDelete /></el-icon>
          <p>无法在浏览器中预览此类文件，请下载后查看</p>
          <el-button type="primary" @click="handleDownload(currentPreviewFile)">
            下载文件
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, defineProps, defineEmits, onMounted, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import axios from 'axios'
import {
  Download, Delete, Edit, Search, Folder, Document,
  DocumentDelete, ArrowDown
} from '@element-plus/icons-vue'

const props = defineProps({
  caseId: {
    type: Number,
    required: true
  },
  refreshTrigger: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['refresh'])

const documents = ref<any[]>([])
const loading = ref(false)
const selectedDocuments = ref<any[]>([])
const categoryFilter = ref('')
const searchQuery = ref('')

// 分类和备注编辑
const categoryDialogVisible = ref(false)
const remarksDialogVisible = ref(false)
const categoryForm = ref({ id: null, category: '' })
const remarksForm = ref({ id: null, remarks: '' })

// 预览相关
const previewDialogVisible = ref(false)
const previewLoading = ref(false)
const previewUrl = ref('')
const currentPreviewFile = ref(null)

// 分类选项示例
const categoryOptions = ref([
  { value: '法律文件', label: '法律文件' },
  { value: '证据材料', label: '证据材料' },
  { value: '客户资料', label: '客户资料' },
  { value: '庭审记录', label: '庭审记录' },
  { value: '其他', label: '其他' }
])

// 文件类型映射
const fileTypeMap: Record<string, string> = {
  'application/pdf': 'PDF',
  'application/msword': 'Word',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Word',
  'application/vnd.ms-excel': 'Excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'Excel',
  'image/jpeg': '图片',
  'image/png': '图片',
  'text/plain': '文本',
  'application/zip': '压缩包',
  'application/x-rar-compressed': '压缩包'
}

// 过滤后的文档列表
const filteredDocuments = computed(() => {
  return documents.value.filter(doc => {
    const matchesCategory = !categoryFilter.value || doc.category === categoryFilter.value
    const matchesSearch = !searchQuery.value || 
      doc.fileName.toLowerCase().includes(searchQuery.value.toLowerCase())
    return matchesCategory && matchesSearch
  })
})

// 获取文档列表
const fetchDocuments = async () => {
  try {
    loading.value = true
    const response = await axios.get(`/api/documents?caseId=${props.caseId}`)
    documents.value = response.data
  } catch (error) {
    console.error('获取文档列表失败:', error)
    ElMessage.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

// 格式化文件大小
const formatFileSize = (size: number) => {
  if (!size) return '0 B'
  
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  
  return `${size.toFixed(2)} ${units[i]}`
}

// 格式化日期
const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).replace(',', '')
}

// 获取文件类型标签
const getFileTypeLabel = (mimeType: string) => {
  return fileTypeMap[mimeType] || '其他'
}

// 判断是否可以在浏览器中预览
const canPreviewInBrowser = (file: any) => {
  if (!file) return false
  
  const previewableTypes = [
    'application/pdf',
    'image/jpeg',
    'image/png',
    'image/gif',
    'text/plain',
    'text/html'
  ]
  
  return previewableTypes.includes(file.fileType)
}

// 表格选择变化
const handleSelectionChange = (selection: any[]) => {
  selectedDocuments.value = selection
}

// 筛选变化
const handleFilterChange = () => {
  // 仅触发过滤条件重新计算
}

// 下载单个文件
const handleDownload = (row: any) => {
  const downloadUrl = `/api/documents/${row.id}/download`
  
  // 创建隐藏的a标签实现下载
  const link = document.createElement('a')
  link.href = downloadUrl
  link.setAttribute('download', row.fileName)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 批量下载
const handleBatchDownload = async () => {
  if (selectedDocuments.value.length === 0) return
  
  try {
    const ids = selectedDocuments.value.map(doc => doc.id)
    
    // 使用axios直接下载
    const response = await axios.post('/api/documents/batch-download', 
      { ids }, 
      { responseType: 'blob' }
    )
    
    // 创建blob链接并触发下载
    const blob = new Blob([response.data])
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.setAttribute('download', 'documents.zip')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    
    ElMessage.success('文件打包下载中')
  } catch (error) {
    console.error('批量下载失败:', error)
    ElMessage.error('批量下载失败')
  }
}

// 预览文件
const handlePreview = async (row: any) => {
  try {
    previewLoading.value = true
    currentPreviewFile.value = row
    previewDialogVisible.value = true
    
    // 对于可预览的文件，获取预览URL
    if (canPreviewInBrowser(row)) {
      previewUrl.value = `/api/documents/${row.id}/preview`
    } else {
      previewUrl.value = ''
    }
  } catch (error) {
    console.error('预览失败:', error)
    ElMessage.error('预览失败')
  } finally {
    previewLoading.value = false
  }
}

// 打开分类对话框
const openCategoryDialog = (row: any) => {
  categoryForm.value.id = row.id
  categoryForm.value.category = row.category || ''
  categoryDialogVisible.value = true
}

// 打开备注对话框
const openRemarksDialog = (row: any) => {
  remarksForm.value.id = row.id
  remarksForm.value.remarks = row.remarks || ''
  remarksDialogVisible.value = true
}

// 更新分类
const updateCategory = async () => {
  try {
    await axios.put(`/api/documents/${categoryForm.value.id}/category`, {
      category: categoryForm.value.category
    })
    
    // 更新本地数据
    const index = documents.value.findIndex(doc => doc.id === categoryForm.value.id)
    if (index !== -1) {
      documents.value[index].category = categoryForm.value.category
    }
    
    ElMessage.success('分类更新成功')
    categoryDialogVisible.value = false
    emit('refresh')
  } catch (error) {
    console.error('更新分类失败:', error)
    ElMessage.error('更新分类失败')
  }
}

// 更新备注
const updateRemarks = async () => {
  try {
    await axios.put(`/api/documents/${remarksForm.value.id}/remarks`, {
      remarks: remarksForm.value.remarks
    })
    
    // 更新本地数据
    const index = documents.value.findIndex(doc => doc.id === remarksForm.value.id)
    if (index !== -1) {
      documents.value[index].remarks = remarksForm.value.remarks
    }
    
    ElMessage.success('备注更新成功')
    remarksDialogVisible.value = false
    emit('refresh')
  } catch (error) {
    console.error('更新备注失败:', error)
    ElMessage.error('更新备注失败')
  }
}

// 确认删除
const confirmDelete = (row: any) => {
  ElMessageBox.confirm(
    `确定要删除文件 "${row.fileName}" 吗？此操作不可恢复!`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    deleteDocument(row.id)
  }).catch(() => {
    // 取消删除
  })
}

// 确认批量删除
const confirmBatchDelete = () => {
  if (selectedDocuments.value.length === 0) return
  
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedDocuments.value.length} 个文件吗？此操作不可恢复!`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    batchDeleteDocuments()
  }).catch(() => {
    // 取消删除
  })
}

// 删除文档
const deleteDocument = async (id: number) => {
  try {
    await axios.delete(`/api/documents/${id}`)
    
    // 更新本地数据
    documents.value = documents.value.filter(doc => doc.id !== id)
    
    ElMessage.success('文件删除成功')
    emit('refresh')
  } catch (error) {
    console.error('删除文件失败:', error)
    ElMessage.error('删除文件失败')
  }
}

// 批量删除文档
const batchDeleteDocuments = async () => {
  try {
    const ids = selectedDocuments.value.map(doc => doc.id)
    await axios.post('/api/documents/batch-delete', { ids })
    
    // 更新本地数据
    documents.value = documents.value.filter(doc => !ids.includes(doc.id))
    
    ElMessage.success('批量删除成功')
    emit('refresh')
  } catch (error) {
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败')
  }
}

// 监听刷新触发器
watch(() => props.refreshTrigger, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    fetchDocuments()
  }
})

// 组件挂载时获取文档列表
onMounted(() => {
  fetchDocuments()
})
</script>

<style scoped>
.document-list {
  width: 100%;
}

.document-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.right-actions {
  display: flex;
  gap: 10px;
}

.file-name-cell {
  display: flex;
  align-items: center;
}

.file-icon {
  margin-right: 8px;
  color: #409EFF;
}

.file-name {
  word-break: break-all;
}

.remarks-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.no-remarks {
  color: #909399;
  font-style: italic;
}

.preview-container {
  min-height: 600px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-error {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}

.preview-error .el-icon {
  font-size: 48px;
  margin-bottom: 20px;
}
</style> 