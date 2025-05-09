<template>
  <div class="document-preview" v-loading="loading">
    <!-- PDF预览 -->
    <template v-if="isPreviewablePdf">
      <iframe
        v-if="previewUrl"
        :src="previewUrl"
        class="pdf-preview"
        frameborder="0"
      ></iframe>
    </template>
    
    <!-- 图片预览 -->
    <template v-else-if="isPreviewableImage">
      <el-image
        v-if="previewUrl"
        :src="previewUrl"
        class="image-preview"
        fit="contain"
        :preview-src-list="[previewUrl]"
        :initial-index="0"
        preview-teleported
      />
    </template>
    
    <!-- 文本预览 -->
    <template v-else-if="isPreviewableText">
      <div v-if="textContent" class="text-preview">
        <pre>{{ textContent }}</pre>
      </div>
    </template>
    
    <!-- 不支持预览的文件类型 -->
    <div v-else class="unsupported-preview">
      <el-icon class="unsupported-icon"><Document /></el-icon>
      <h3>{{ document ? getFileTypeText() : '未选择文件' }}</h3>
      <p>{{ document ? '此类型文件暂不支持在线预览' : '请选择要预览的文件' }}</p>
      <el-button 
        v-if="document" 
        type="primary" 
        @click="handleDownload"
      >
        下载文件
      </el-button>
    </div>
    
    <!-- 预览控制栏 -->
    <div v-if="document" class="preview-controls">
      <div class="file-info">
        <span class="file-name">{{ document.fileName }}</span>
        <span class="file-size">({{ formatFileSize(document.fileSize) }})</span>
      </div>
      <div class="control-buttons">
        <el-button type="primary" size="small" @click="handleDownload">
          <el-icon><Download /></el-icon> 下载
        </el-button>
        <el-tooltip content="在新窗口打开" placement="top">
          <el-button v-if="canOpenInNewWindow" type="info" size="small" @click="openInNewWindow">
            <el-icon><TopRight /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, watch, defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { saveAs } from 'file-saver'
import { Document, Download, TopRight } from '@element-plus/icons-vue'
import { safeDownloadDocument, safePreviewDocument } from '../../api/document'

const props = defineProps({
  document: {
    type: Object as () => any,
    default: null
  },
  autoLoad: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['download'])

// 状态变量
const loading = ref(false)
const previewUrl = ref('')
const textContent = ref('')

// 计算属性：判断文件类型
const fileType = computed(() => props.document?.fileType || '')

const isPreviewablePdf = computed(() => 
  fileType.value === 'application/pdf'
)

const isPreviewableImage = computed(() => 
  fileType.value?.startsWith('image/') || false
)

const isPreviewableText = computed(() => {
  const textTypes = [
    'text/plain', 
    'text/html', 
    'text/css', 
    'text/javascript',
    'application/json',
    'application/xml'
  ]
  return textTypes.includes(fileType.value)
})

const canOpenInNewWindow = computed(() => 
  isPreviewablePdf.value || isPreviewableImage.value
)

// 获取文件类型显示文本
const getFileTypeText = () => {
  const fileTypeMap: Record<string, string> = {
    'application/pdf': 'PDF文档',
    'application/msword': 'Word文档',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Word文档',
    'application/vnd.ms-excel': 'Excel表格',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'Excel表格',
    'image/jpeg': 'JPEG图片',
    'image/png': 'PNG图片',
    'image/gif': 'GIF图片',
    'text/plain': '文本文件',
    'text/html': 'HTML文件',
    'application/zip': 'ZIP压缩文件',
    'application/x-rar-compressed': 'RAR压缩文件',
    'video/mp4': 'MP4视频',
    'audio/mpeg': 'MP3音频'
  }
  
  return fileTypeMap[fileType.value] || '未知类型文件'
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

// 加载预览内容
const loadPreview = async () => {
  if (!props.document) return
  
  try {
    loading.value = true
    previewUrl.value = ''
    textContent.value = ''
    
    // PDF, 图片等直接生成URL
    if (isPreviewablePdf.value || isPreviewableImage.value) {
      const response = await safePreviewDocument(props.document.id)
      if (!response) return
      
      const blob = new Blob([response.data], { type: fileType.value })
      previewUrl.value = URL.createObjectURL(blob)
    } 
    // 文本类型需要转换为文本
    else if (isPreviewableText.value) {
      const response = await safePreviewDocument(props.document.id)
      if (!response) return
      
      const reader = new FileReader()
      reader.onload = e => {
        textContent.value = e.target?.result as string || ''
      }
      reader.readAsText(new Blob([response.data]))
    }
  } catch (error) {
    console.error('预览文件失败:', error)
    ElMessage.error('预览文件失败')
  } finally {
    loading.value = false
  }
}

// 下载文件
const handleDownload = async () => {
  if (!props.document) return
  
  try {
    loading.value = true
    const response = await safeDownloadDocument(props.document.id)
    if (!response) return
    
    const blob = new Blob([response.data], { type: fileType.value })
    saveAs(blob, props.document.fileName)
    
    emit('download', props.document)
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  } finally {
    loading.value = false
  }
}

// 在新窗口打开
const openInNewWindow = () => {
  if (!previewUrl.value) return
  window.open(previewUrl.value, '_blank')
}

// 清理资源
const cleanup = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
  textContent.value = ''
}

// 监听文档变化
watch(() => props.document, (newDoc) => {
  cleanup()
  if (newDoc && props.autoLoad) {
    loadPreview()
  }
}, { immediate: true })

// 组件销毁前清理
onMounted(() => {
  return cleanup
})
</script>

<style scoped>
.document-preview {
  width: 100%;
  height: 100%;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  position: relative;
}

.pdf-preview,
.image-preview {
  width: 100%;
  height: calc(100% - 50px);
  min-height: 350px;
  object-fit: contain;
}

.text-preview {
  width: 100%;
  height: calc(100% - 50px);
  min-height: 350px;
  overflow: auto;
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
}

.text-preview pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', Courier, monospace;
}

.unsupported-preview {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: calc(100% - 50px);
  min-height: 350px;
  color: #909399;
}

.unsupported-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #c0c4cc;
}

.preview-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-top: 1px solid #ebeef5;
  margin-top: auto;
  background-color: #f5f7fa;
}

.file-info {
  display: flex;
  align-items: center;
}

.file-name {
  font-weight: 500;
  margin-right: 8px;
}

.file-size {
  color: #909399;
  font-size: 0.9em;
}

.control-buttons {
  display: flex;
  gap: 8px;
}
</style> 