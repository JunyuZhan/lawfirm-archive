<template>
  <div class="case-detail">
    <div class="page-header">
      <div class="title-section">
        <el-button icon="el-icon-back" size="small" @click="goBack">返回</el-button>
        <h1>案件档案详情</h1>
      </div>
      <div class="action-buttons">
        <el-button type="primary" @click="editCase">编辑案件</el-button>
        <el-button type="success" @click="uploadDocuments">上传文件</el-button>
      </div>
    </div>

    <!-- 案件基本信息卡片 -->
    <el-card class="case-info-card">
      <template #header>
        <div class="card-header">
          <h2>基本信息</h2>
          <el-tag size="medium" :type="getStatusType(caseInfo.status)">{{ getStatusLabel(caseInfo.status) }}</el-tag>
        </div>
      </template>
      <div class="case-info-content">
        <div class="info-row">
          <div class="info-item">
            <div class="info-label">案号</div>
            <div class="info-value">{{ caseInfo.caseNo }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">案件类型</div>
            <div class="info-value">{{ getCaseType(caseInfo.type) }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">案由</div>
            <div class="info-value">{{ caseInfo.title }}</div>
          </div>
        </div>
        <div class="info-row">
          <div class="info-item">
            <div class="info-label">当事人</div>
            <div class="info-value">{{ caseInfo.parties }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">管辖法院</div>
            <div class="info-value">{{ caseInfo.court }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">立案日期</div>
            <div class="info-value">{{ caseInfo.filingDate }}</div>
          </div>
        </div>
        <div class="info-row full-width">
          <div class="info-item">
            <div class="info-label">备注</div>
            <div class="info-value remarks">{{ caseInfo.remarks || '暂无备注' }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 档案文件列表 -->
    <el-card class="documents-card">
      <template #header>
        <div class="card-header">
          <h2>档案文件</h2>
          <div class="header-actions">
            <el-input
              v-model="fileSearchKeyword"
              placeholder="搜索文件名"
              prefix-icon="el-icon-search"
              size="small"
              style="width: 200px"
              clearable
            />
            <el-button type="primary" size="small" @click="uploadDocuments">上传文件</el-button>
          </div>
        </div>
      </template>

      <!-- 文件分类标签 -->
      <el-tabs v-model="activeFileTab">
        <el-tab-pane label="全部文件" name="all"></el-tab-pane>
        <el-tab-pane label="证据材料" name="evidence"></el-tab-pane>
        <el-tab-pane label="诉讼文书" name="litigation"></el-tab-pane>
        <el-tab-pane label="裁判文书" name="judgment"></el-tab-pane>
        <el-tab-pane label="其他材料" name="others"></el-tab-pane>
      </el-tabs>

      <!-- 文件列表 -->
      <el-table :data="filteredDocuments" stripe style="width: 100%;" v-loading="documentsLoading">
        <el-table-column prop="fileName" label="文件名称" min-width="200">
          <template #default="scope">
            <div class="file-name-cell">
              <i :class="getFileIcon(scope.row.fileType)" class="file-icon"></i>
              <el-link @click="previewFile(scope.row)">{{ scope.row.fileName }}</el-link>
              <el-tag v-if="scope.row.versions > 1" size="mini" type="info">v{{ scope.row.versions }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="scope">
            <el-tag size="small" :type="getFileTagType(scope.row.category)">
              {{ getFileCategoryName(scope.row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column prop="fileSize" label="大小" width="100" />
        <el-table-column prop="uploadedBy" label="上传人" width="120" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="previewFile(scope.row)">预览</el-button>
            <el-button size="small" type="primary" @click="downloadFile(scope.row)">下载</el-button>
            <el-dropdown trigger="click" size="small" @command="(cmd) => handleFileAction(cmd, scope.row)">
              <el-button size="small">
                更多<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="history">版本历史</el-dropdown-item>
                  <el-dropdown-item command="rename">重命名</el-dropdown-item>
                  <el-dropdown-item command="changeCategory">更改分类</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 文件分页 -->
      <div class="pagination">
        <el-pagination
          @size-change="handleFileSizeChange"
          @current-change="handleFilePageChange"
          :current-page="filePage"
          :page-sizes="[10, 20, 50]"
          :page-size="filePageSize"
          layout="total, sizes, prev, pager, next"
          :total="totalDocuments"
        />
      </div>
    </el-card>

    <!-- 文件上传对话框 -->
    <el-dialog title="上传档案文件" v-model="uploadDialogVisible" width="600px">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="文件分类">
          <el-select v-model="uploadForm.category" placeholder="请选择文件分类">
            <el-option label="证据材料" value="evidence" />
            <el-option label="诉讼文书" value="litigation" />
            <el-option label="裁判文书" value="judgment" />
            <el-option label="其他材料" value="others" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件上传">
          <el-upload
            :action="`${baseApiUrl}/api/documents/upload/${caseId}`"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :data="{category: uploadForm.category, remarks: uploadForm.remarks}"
            :file-list="uploadForm.fileList"
            multiple
          >
            <el-button size="small" type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">允许上传PDF、Word、图片等格式文件</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="uploadForm.remarks" type="textarea" rows="3"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="uploadDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmUpload" :loading="uploading">上传</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 版本历史对话框 -->
    <el-dialog title="文件版本历史" v-model="historyDialogVisible" width="700px">
      <div v-if="selectedFile" class="file-history-header">
        <h3>{{ selectedFile.fileName }}</h3>
      </div>
      <el-table :data="fileVersions" stripe style="width: 100%;" v-loading="versionsLoading">
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column prop="uploadedBy" label="上传人" width="120" />
        <el-table-column prop="remarks" label="备注" />
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="previewVersion(scope.row)">预览</el-button>
            <el-button size="small" type="primary" @click="restoreVersion(scope.row)" v-if="scope.row.version !== 'current'">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog title="重命名文件" v-model="renameDialogVisible" width="500px">
      <el-form :model="renameForm" label-width="100px">
        <el-form-item label="新文件名">
          <el-input v-model="renameForm.newName" placeholder="请输入新文件名"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="renameDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmRename" :loading="renaming">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 更改分类对话框 -->
    <el-dialog title="更改文件分类" v-model="categoryDialogVisible" width="500px">
      <el-form :model="categoryForm" label-width="100px">
        <el-form-item label="选择分类">
          <el-select v-model="categoryForm.newCategory" placeholder="请选择文件分类">
            <el-option label="证据材料" value="evidence" />
            <el-option label="诉讼文书" value="litigation" />
            <el-option label="裁判文书" value="judgment" />
            <el-option label="其他材料" value="others" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="categoryDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmChangeCategory" :loading="changingCategory">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import axios from 'axios';
import { useUserStore } from '../stores/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const caseId = route.params.id;
const baseApiUrl = import.meta.env.VITE_API_BASE_URL || '';

// 上传所需的headers
const uploadHeaders = {
  Authorization: `Bearer ${userStore.token}`
};

// 案件基本信息
const caseInfo = reactive({
  id: caseId,
  caseNo: '',
  title: '',
  type: '',
  parties: '',
  court: '',
  filingDate: '',
  status: '',
  remarks: ''
});

// 文件相关
const documentsLoading = ref(false);
const documents = ref([]);
const fileSearchKeyword = ref('');
const activeFileTab = ref('all');
const filePage = ref(1);
const filePageSize = ref(10);
const totalDocuments = ref(0);

// 上传对话框
const uploadDialogVisible = ref(false);
const uploading = ref(false);
const uploadForm = reactive({
  category: 'evidence',
  fileList: [],
  remarks: ''
});

// 版本历史
const historyDialogVisible = ref(false);
const versionsLoading = ref(false);
const fileVersions = ref([]);
const selectedFile = ref(null);

// 重命名对话框
const renameDialogVisible = ref(false);
const renaming = ref(false);
const renameForm = reactive({
  newName: '',
  fileId: null
});

// 更改分类对话框
const categoryDialogVisible = ref(false);
const changingCategory = ref(false);
const categoryForm = reactive({
  newCategory: '',
  fileId: null
});

// 计算过滤后的文档列表
const filteredDocuments = computed(() => {
  let filtered = documents.value;
  
  // 按分类过滤
  if (activeFileTab.value !== 'all') {
    filtered = filtered.filter(doc => doc.category === activeFileTab.value);
  }
  
  // 按关键字搜索
  if (fileSearchKeyword.value) {
    const keyword = fileSearchKeyword.value.toLowerCase();
    filtered = filtered.filter(doc => 
      doc.fileName.toLowerCase().includes(keyword)
    );
  }
  
  return filtered;
});

// API服务类
const apiService = {
  // 获取案件详情
  async getCaseDetail(caseId) {
    try {
      const response = await axios.get(`${baseApiUrl}/api/cases/${caseId}`, {
        headers: { Authorization: `Bearer ${userStore.token}` }
      });
      return response.data;
    } catch (error) {
      console.error('获取案件详情失败:', error);
      throw error;
    }
  },
  
  // 获取案件档案文件列表
  async getDocuments(caseId, page, pageSize) {
    try {
      const response = await axios.get(`${baseApiUrl}/api/documents`, {
        params: { caseId, page, pageSize },
        headers: { Authorization: `Bearer ${userStore.token}` }
      });
      return response.data;
    } catch (error) {
      console.error('获取文件列表失败:', error);
      throw error;
    }
  },
  
  // 获取文件版本历史
  async getFileVersions(fileId) {
    try {
      const response = await axios.get(`${baseApiUrl}/api/documents/${fileId}/versions`, {
        headers: { Authorization: `Bearer ${userStore.token}` }
      });
      return response.data;
    } catch (error) {
      console.error('获取版本历史失败:', error);
      throw error;
    }
  },
  
  // 恢复文件版本
  async restoreFileVersion(fileId, versionId) {
    try {
      const response = await axios.post(`${baseApiUrl}/api/documents/${fileId}/versions/${versionId}/restore`, {}, {
        headers: { Authorization: `Bearer ${userStore.token}` }
      });
      return response.data;
    } catch (error) {
      console.error('恢复版本失败:', error);
      throw error;
    }
  },
  
  // 重命名文件
  async renameFile(fileId, newName) {
    try {
      const response = await axios.put(`${baseApiUrl}/api/documents/${fileId}/rename`, { newName }, {
        headers: { Authorization: `Bearer ${userStore.token}` }
      });
      return response.data;
    } catch (error) {
      console.error('重命名文件失败:', error);
      throw error;
    }
  },
  
  // 更改文件分类
  async changeFileCategory(fileId, newCategory) {
    try {
      const response = await axios.put(`${baseApiUrl}/api/documents/${fileId}/category`, { category: newCategory }, {
        headers: { Authorization: `Bearer ${userStore.token}` }
      });
      return response.data;
    } catch (error) {
      console.error('更改文件分类失败:', error);
      throw error;
    }
  },
  
  // 删除文件
  async deleteFile(fileId) {
    try {
      const response = await axios.delete(`${baseApiUrl}/api/documents/${fileId}`, {
        headers: { Authorization: `Bearer ${userStore.token}` }
      });
      return response.data;
    } catch (error) {
      console.error('删除文件失败:', error);
      throw error;
    }
  },
  
  // 获取文件下载链接
  getFileDownloadUrl(fileId) {
    return `${baseApiUrl}/api/documents/${fileId}/download?token=${userStore.token}`;
  },
  
  // 获取文件预览链接
  getFilePreviewUrl(fileId) {
    return `${baseApiUrl}/api/documents/${fileId}/preview?token=${userStore.token}`;
  },
  
  // 获取文件版本预览链接
  getVersionPreviewUrl(fileId, versionId) {
    return `${baseApiUrl}/api/documents/${fileId}/versions/${versionId}/preview?token=${userStore.token}`;
  }
};

// 获取案件类型名称
const getCaseType = (type) => {
  const types = {
    'civil': '民事',
    'criminal': '刑事',
    'administrative': '行政',
    'non_litigation': '非诉'
  };
  return types[type] || type;
};

// 获取案件状态
const getStatusLabel = (status) => {
  const statusMap = {
    'active': '进行中',
    'closed': '已结案',
    'archived': '已归档'
  };
  return statusMap[status] || status;
};

const getStatusType = (status) => {
  const typeMap = {
    'active': 'primary',
    'closed': 'success',
    'archived': 'info'
  };
  return typeMap[status] || '';
};

// 获取文件分类名称
const getFileCategoryName = (category) => {
  const categories = {
    'evidence': '证据材料',
    'litigation': '诉讼文书',
    'judgment': '裁判文书',
    'others': '其他材料'
  };
  return categories[category] || category;
};

// 获取文件分类标签类型
const getFileTagType = (category) => {
  const typeMap = {
    'evidence': '',
    'litigation': 'success',
    'judgment': 'warning',
    'others': 'info'
  };
  return typeMap[category] || '';
};

// 根据文件类型获取图标
const getFileIcon = (fileType) => {
  const iconMap = {
    'pdf': 'el-icon-document',
    'doc': 'el-icon-document-word',
    'docx': 'el-icon-document-word',
    'xls': 'el-icon-document-excel',
    'xlsx': 'el-icon-document-excel',
    'ppt': 'el-icon-document-ppt',
    'pptx': 'el-icon-document-ppt',
    'jpg': 'el-icon-picture',
    'jpeg': 'el-icon-picture',
    'png': 'el-icon-picture',
    'zip': 'el-icon-document-zip',
    'rar': 'el-icon-document-zip'
  };
  return iconMap[fileType] || 'el-icon-document';
};

// 返回上一页
const goBack = () => {
  router.push('/cases');
};

// 编辑案件
const editCase = () => {
  router.push(`/cases/${caseId}/edit`);
};

// 上传文件
const uploadDocuments = () => {
  uploadForm.fileList = [];
  uploadForm.category = 'evidence';
  uploadForm.remarks = '';
  uploadDialogVisible.value = true;
};

// 文件上传成功处理
const handleUploadSuccess = (response, file, fileList) => {
  ElMessage.success(`文件 ${file.name} 上传成功`);
  if (fileList.every(f => f.status === 'success' || f.status === 'fail')) {
    uploadDialogVisible.value = false;
    fetchDocuments(); // 刷新文件列表
  }
};

// 文件上传失败处理
const handleUploadError = (error, file) => {
  console.error('上传文件失败:', error);
  ElMessage.error(`文件 ${file.name} 上传失败`);
};

// 确认上传
const confirmUpload = async () => {
  if (uploadForm.fileList.length === 0) {
    ElMessage.warning('请选择要上传的文件');
    return;
  }

  const uploadEl = document.querySelector('.el-upload__input');
  if (uploadEl) {
    uploadEl.click();
  }
};

// 文件操作
const handleFileAction = (action, file) => {
  selectedFile.value = file;
  
  switch (action) {
    case 'history':
      showFileHistory(file);
      break;
    case 'rename':
      showRenameDialog(file);
      break;
    case 'changeCategory':
      showChangeCategoryDialog(file);
      break;
    case 'delete':
      confirmDeleteFile(file);
      break;
  }
};

// 显示文件版本历史
const showFileHistory = async (file) => {
  historyDialogVisible.value = true;
  versionsLoading.value = true;
  
  try {
    const response = await apiService.getFileVersions(file.id);
    fileVersions.value = response.versions;
  } catch (error) {
    ElMessage.error('获取版本历史失败');
  } finally {
    versionsLoading.value = false;
  }
};

// 显示重命名对话框
const showRenameDialog = (file) => {
  renameForm.newName = file.fileName;
  renameForm.fileId = file.id;
  renameDialogVisible.value = true;
};

// 确认重命名
const confirmRename = async () => {
  if (!renameForm.newName) {
    ElMessage.warning('请输入新文件名');
    return;
  }
  
  renaming.value = true;
  
  try {
    await apiService.renameFile(renameForm.fileId, renameForm.newName);
    ElMessage.success('文件重命名成功');
    renameDialogVisible.value = false;
    fetchDocuments(); // 刷新文件列表
  } catch (error) {
    ElMessage.error('文件重命名失败');
  } finally {
    renaming.value = false;
  }
};

// 显示更改分类对话框
const showChangeCategoryDialog = (file) => {
  categoryForm.newCategory = file.category;
  categoryForm.fileId = file.id;
  categoryDialogVisible.value = true;
};

// 确认更改分类
const confirmChangeCategory = async () => {
  changingCategory.value = true;
  
  try {
    await apiService.changeFileCategory(categoryForm.fileId, categoryForm.newCategory);
    ElMessage.success('文件分类已更改');
    categoryDialogVisible.value = false;
    fetchDocuments(); // 刷新文件列表
  } catch (error) {
    ElMessage.error('更改文件分类失败');
  } finally {
    changingCategory.value = false;
  }
};

// 预览文件
const previewFile = (file) => {
  const url = apiService.getFilePreviewUrl(file.id);
  window.open(url, '_blank');
};

// 预览版本文件
const previewVersion = (version) => {
  if (!selectedFile.value) return;
  
  const url = apiService.getVersionPreviewUrl(selectedFile.value.id, version.id);
  window.open(url, '_blank');
};

// 恢复版本
const restoreVersion = async (version) => {
  if (!selectedFile.value) return;
  
  try {
    ElMessageBox.confirm(`确定要恢复到版本 ${version.version} 吗？`, '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    await apiService.restoreFileVersion(selectedFile.value.id, version.id);
    ElMessage.success(`已恢复到版本 ${version.version}`);
    historyDialogVisible.value = false;
    fetchDocuments(); // 刷新文件列表
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('恢复版本失败');
    }
  }
};

// 确认删除文件
const confirmDeleteFile = (file) => {
  ElMessageBox.confirm(`确定要删除文件 "${file.fileName}" 吗？此操作不可逆`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await apiService.deleteFile(file.id);
      ElMessage.success(`文件 "${file.fileName}" 已删除`);
      fetchDocuments(); // 刷新文件列表
    } catch (error) {
      ElMessage.error('删除文件失败');
    }
  }).catch(() => {});
};

// 下载文件
const downloadFile = (file) => {
  const url = apiService.getFileDownloadUrl(file.id);
  const link = document.createElement('a');
  link.href = url;
  link.download = file.fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  ElMessage.success(`开始下载: ${file.fileName}`);
};

// 文件分页变化
const handleFileSizeChange = (newSize) => {
  filePageSize.value = newSize;
  fetchDocuments();
};

const handleFilePageChange = (newPage) => {
  filePage.value = newPage;
  fetchDocuments();
};

// 获取案件详情
const fetchCaseDetail = async () => {
  try {
    const data = await apiService.getCaseDetail(caseId);
    Object.assign(caseInfo, data);
  } catch (error) {
    ElMessage.error('获取案件详情失败');
  }
};

// 获取案件档案文件
const fetchDocuments = async () => {
  documentsLoading.value = true;
  
  try {
    const data = await apiService.getDocuments(caseId, filePage.value, filePageSize.value);
    documents.value = data.documents;
    totalDocuments.value = data.total;
  } catch (error) {
    ElMessage.error('获取文件列表失败');
  } finally {
    documentsLoading.value = false;
  }
};

onMounted(() => {
  fetchCaseDetail();
  fetchDocuments();
});
</script>

<style scoped>
.case-detail {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 15px;
}

.title-section h1 {
  margin: 0;
  font-size: 22px;
  color: #333;
}

.case-info-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.case-info-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-row {
  display: flex;
  gap: 30px;
}

.info-row.full-width {
  width: 100%;
}

.info-item {
  flex: 1;
  min-width: 0;
}

.info-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

.info-value {
  font-size: 15px;
  color: #333;
  word-break: break-word;
}

.info-value.remarks {
  white-space: pre-line;
  line-height: 1.5;
}

.documents-card {
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 16px;
  color: #606266;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.file-history-header {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.file-history-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}
</style> 