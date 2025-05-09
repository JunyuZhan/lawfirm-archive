<template>
  <div class="document-uploader">
    <el-upload
      ref="uploadRef"
      drag
      multiple
      :auto-upload="false"
      :on-change="handleFileChange"
      :file-list="fileList"
      :limit="10"
      :on-exceed="handleExceed"
      :on-remove="handleRemove"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">支持各类文件格式，文件大小不限</div>
      </template>
    </el-upload>

    <div class="upload-options">
      <el-form :model="uploadOptions" label-width="80px" size="small">
        <el-form-item label="文件分类">
          <el-input v-model="uploadOptions.category" placeholder="请输入分类"></el-input>
        </el-form-item>
        <el-form-item label="文件备注">
          <el-input v-model="uploadOptions.remarks" placeholder="请输入备注"></el-input>
        </el-form-item>
      </el-form>
    </div>

    <div class="upload-progress" v-if="isUploading">
      <div v-for="(task, index) in uploadTasks" :key="index" class="task-item">
        <div class="task-info">
          <span class="file-name">{{ task.fileName }}</span>
          <span class="progress-text">{{ getProgressText(task) }}</span>
        </div>
        <el-progress 
          :percentage="calcProgress(task)"
          :status="getProgressStatus(task)"
        ></el-progress>
        <div class="task-actions" v-if="task.status !== 'success' && task.status !== 'exception'">
          <el-button size="small" type="danger" @click="cancelUpload(task)" :loading="task.status === 'cancelling'">
            取消
          </el-button>
        </div>
      </div>
    </div>

    <div class="upload-actions">
      <el-button type="primary" @click="startUpload" :loading="isUploading">开始上传</el-button>
      <el-button @click="resetUploader">重置</el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue';
import { ElNotification, ElMessageBox } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import type { UploadInstance, UploadUserFile } from 'element-plus';
import {
  safeInitChunkedUpload,
  safeUploadChunk,
  safeMergeChunks,
  safeCancelUpload
} from '../../api/document';

const props = defineProps({
  caseId: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['upload-success', 'upload-error']);

// 上传参数
const uploadOptions = reactive({
  category: '',
  remarks: ''
});

// 上传状态
const fileList = ref<UploadUserFile[]>([]);
const isUploading = ref(false);
const uploadRef = ref<UploadInstance>();

// 分片大小定义（5MB）
const CHUNK_SIZE = 5 * 1024 * 1024;

// 定义上传任务类型和状态类型
type UploadTaskStatus = 'pending' | 'uploading' | 'merging' | 'success' | 'exception' | 'cancelling';

interface UploadTask {
  file: File;
  fileName: string;
  fileSize: number;
  taskId: string;
  totalChunks: number;
  uploadedChunks: number;
  status: UploadTaskStatus;
  error?: string;
}

// 上传任务列表
const uploadTasks = ref<UploadTask[]>([]);

// 文件选择事件
const handleFileChange = (file: UploadUserFile) => {
  console.log('File selected:', file);
};

// 超出文件数限制事件
const handleExceed = () => {
  ElNotification({
    title: '警告',
    message: '最多只能上传10个文件',
    type: 'warning'
  });
};

// 移除文件事件
const handleRemove = (file: UploadUserFile) => {
  // 移除上传任务
  const index = uploadTasks.value.findIndex(task => 
    task.fileName === file.name && task.fileSize === file.size
  );
  
  if (index !== -1) {
    // 如果任务已经在进行中，需要取消任务
    const task = uploadTasks.value[index];
    if (task.taskId && (task.status === 'uploading' || task.status === 'pending')) {
      cancelUpload(task);
    }
    uploadTasks.value.splice(index, 1);
  }
};

// 计算上传进度
const calcProgress = (task: UploadTask) => {
  if (task.status === 'success') return 100;
  if (task.status === 'merging') return 99;
  if (task.totalChunks === 0) return 0;
  return Math.floor((task.uploadedChunks / task.totalChunks) * 98);
};

// 获取进度状态
const getProgressStatus = (task: UploadTask) => {
  if (task.status === 'success') return 'success';
  if (task.status === 'exception') return 'exception';
  return '';
};

// 获取进度文本
const getProgressText = (task: UploadTask) => {
  if (task.status === 'pending') return '等待上传';
  if (task.status === 'uploading') return `上传中 ${calcProgress(task)}%`;
  if (task.status === 'merging') return '合并中 99%';
  if (task.status === 'success') return '上传成功';
  if (task.status === 'exception') return `上传失败: ${task.error || '未知错误'}`;
  if (task.status === 'cancelling') return '取消中...';
  return '';
};

// 开始上传
const startUpload = async () => {
  if (fileList.value.length === 0) {
    ElNotification({
      title: '提示',
      message: '请选择要上传的文件',
      type: 'info'
    });
    return;
  }

  isUploading.value = true;
  
  // 初始化上传任务
  uploadTasks.value = fileList.value.map(file => ({
    file: file.raw!,
    fileName: file.name,
    fileSize: file.size!,
    taskId: '',
    totalChunks: Math.ceil(file.size! / CHUNK_SIZE),
    uploadedChunks: 0,
    status: 'pending'
  }));
  
  // 同时处理多个文件上传，但每个文件的分片是顺序上传的
  const promises = uploadTasks.value.map(uploadFile);
  await Promise.allSettled(promises);
  
  isUploading.value = false;
  // 完成后通知父组件
  emit('upload-success');
};

// 上传单个文件
const uploadFile = async (task: UploadTask) => {
  try {
    // 初始化分片上传
    task.status = 'uploading';
    const response = await safeInitChunkedUpload(
      task.fileName,
      task.fileSize,
      task.file.type,
      props.caseId,
      uploadOptions.category,
      uploadOptions.remarks
    );
    
    if (response && response.data) {
      task.taskId = response.data.id;
    } else {
      throw new Error('初始化上传失败：没有收到任务ID');
    }
    
    // 分片上传
    for (let i = 1; i <= task.totalChunks; i++) {
      // 检查是否被取消
      if (task.status === 'cancelling') {
        return;
      }
      
      const start = (i - 1) * CHUNK_SIZE;
      const end = Math.min(i * CHUNK_SIZE, task.fileSize);
      const chunk = task.file.slice(start, end);
      
      // 创建一个带名称的文件对象
      const chunkFile = new File([chunk], task.fileName, { type: task.file.type });
      
      // 上传分片
      const chunkResponse = await safeUploadChunk(task.taskId, i, chunkFile);
      
      if (chunkResponse && chunkResponse.data) {
        task.uploadedChunks = chunkResponse.data.uploadedChunks;
        
        // 如果全部分片上传完成，合并分片
        if (chunkResponse.data.completed) {
          task.status = 'merging';
          
          // 合并分片
          const mergeResponse = await safeMergeChunks(task.taskId);
          
          // 标记为成功
          task.status = 'success';
        }
      } else {
        throw new Error('上传分片失败：没有收到响应数据');
      }
    }
  } catch (error: any) {
    console.error('Upload failed:', error);
    task.status = 'exception';
    task.error = error.message || '上传失败';
    emit('upload-error', { task, error });
  }
};

// 取消上传
const cancelUpload = async (task: UploadTask) => {
  try {
    task.status = 'cancelling';
    
    if (task.taskId) {
      await safeCancelUpload(task.taskId);
    }
    
    // 从任务列表中移除
    const index = uploadTasks.value.findIndex(t => t.taskId === task.taskId);
    if (index !== -1) {
      uploadTasks.value.splice(index, 1);
    }
    
    // 从文件列表中移除
    const fileIndex = fileList.value.findIndex(file => 
      file.name === task.fileName && file.size === task.fileSize
    );
    
    if (fileIndex !== -1) {
      fileList.value.splice(fileIndex, 1);
    }
  } catch (error) {
    console.error('Cancel upload failed:', error);
    ElNotification({
      title: '错误',
      message: '取消上传失败',
      type: 'error'
    });
  }
};

// 重置上传器
const resetUploader = () => {
  if (isUploading.value) {
    ElMessageBox.confirm('正在上传中，确定要取消上传吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      // 取消所有上传任务
      uploadTasks.value.forEach(task => {
        if (task.status === 'uploading' || task.status === 'pending') {
          cancelUpload(task);
        }
      });
      
      uploadTasks.value = [];
      fileList.value = [];
      isUploading.value = false;
    }).catch(() => {
      // 取消重置操作
    });
    return;
  }
  
  uploadTasks.value = [];
  fileList.value = [];
  uploadOptions.category = '';
  uploadOptions.remarks = '';
};
</script>

<style scoped>
.document-uploader {
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.upload-options {
  margin-top: 20px;
  padding: 15px;
  border-radius: 4px;
  background-color: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.upload-progress {
  margin-top: 20px;
  padding: 15px;
  border-radius: 4px;
  background-color: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.task-item {
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.task-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.file-name {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.progress-text {
  font-size: 13px;
  color: #909399;
}

.task-actions {
  margin-top: 10px;
  text-align: right;
}

.upload-actions {
  margin-top: 20px;
  text-align: center;
}
</style> 