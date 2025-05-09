<template>
  <div class="case-detail-flex">
    <!-- 左侧树形目录 -->
    <div class="tree-panel">
      <div class="action-buttons">
        <div class="button-group">
          <el-button type="primary" size="small" @click="uploadDocuments">
            <el-icon><Upload /></el-icon>上传文件
          </el-button>
        </div>
        <div class="button-group">
          <el-button type="success" size="small" :disabled="!selectedFiles.length" @click="handleBatchDownload">
            <el-icon><Download /></el-icon>批量下载
          </el-button>
          <el-button type="info" size="small" :disabled="!selectedFiles.length" @click="openBatchRemarksDialog">
            <el-icon><Edit /></el-icon>批量备注
          </el-button>
        </div>
        <div class="button-group">
          <el-button type="primary" size="small" :disabled="!selectedFiles.length" @click="openBatchCategoryDialog">
            <el-icon><Folder /></el-icon>批量分类
          </el-button>
          <el-button type="danger" size="small" :disabled="!selectedFiles.length" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>批量删除
          </el-button>
        </div>
      </div>
      <el-tree
        :data="treeData"
        :props="treeProps"
        node-key="id"
        highlight-current
        show-checkbox
        :default-checked-keys="[]"
        @node-click="handleTreeNodeClick"
        @check-change="handleTreeCheckChange"
        style="height: calc(75vh - 120px); overflow-y: auto; margin-top: 12px;"
        :expand-on-click-node="false"
        ref="treeRef"
        draggable
        @node-drag-end="handleDragEnd"
      >
        <template #default="{ node, data }">
          <el-dropdown trigger="contextmenu" @command="cmd => handleFileContextMenu(cmd, data)">
            <span class="tree-file-label">
              <i :class="getFileIcon(data.fileType)" v-if="data.fileType" style="margin-right:4px;"></i>
              {{ data.label }}
              <el-tag v-if="data.remarks" size="small" type="info" style="margin-left:4px;">有备注</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu v-if="data.fileType">
                <el-dropdown-item command="rename">重命名</el-dropdown-item>
                <el-dropdown-item command="changeCategory">更改分类</el-dropdown-item>
                <el-dropdown-item command="editRemarks">编辑备注</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-tree>
    </div>
    <!-- 右侧预览区 -->
    <div class="preview-panel">
      <div v-if="selectedFile">
        <template v-if="selectedFile.fileType && selectedFile.fileType.startsWith('image/')">
          <el-image :src="getPreviewUrl(selectedFile)" style="max-width:100%;max-height:70vh;" fit="contain" />
        </template>
        <template v-else-if="selectedFile.fileType === 'application/pdf'">
          <iframe :src="getPreviewUrl(selectedFile)" style="width:100%;height:80vh;border:none;"></iframe>
        </template>
        <template v-else>
          <div style="padding:40px 0;text-align:center;">暂不支持在线预览，请下载后查看。</div>
        </template>
        <div style="margin-top:16px;">
          <el-button size="small" type="primary" @click="handleDownload(selectedFile)">下载</el-button>
          <el-button size="small" type="danger" @click="handleDelete(selectedFile)">删除</el-button>
        </div>
        <el-divider />
        <div style="margin-top:12px;">
          <div style="font-weight:600;">备注信息：</div>
          <el-input
            v-model="selectedFile.remarks"
            type="textarea"
            rows="3"
            placeholder="请输入备注信息"
            style="margin-top:6px;max-width:500px;"
            @change="saveFileRemarks(selectedFile)"
          />
        </div>
      </div>
      <div v-else style="padding:40px 0;text-align:center;color:#aaa;">请选择左侧文件进行预览</div>
    </div>
  </div>
  <!-- 上传对话框等保留 -->
  <el-dialog title="上传档案文件" v-model="uploadDialogVisible" width="700px">
    <document-uploader 
      :case-id="Number(caseId)" 
      @upload-success="handleUploadComplete" 
      @upload-error="handleUploadError"
    />
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="uploadDialogVisible = false">关闭</el-button>
      </span>
    </template>
  </el-dialog>
  <el-dialog title="重命名文件" v-model="renameDialogVisible" width="400px">
    <el-form :model="renameForm" label-width="80px">
      <el-form-item label="新文件名">
        <el-input v-model="renameForm.newName" placeholder="请输入新文件名"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="renameDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmRename">确定</el-button>
    </template>
  </el-dialog>
  <el-dialog title="更改文件分类" v-model="categoryDialogVisible" width="400px">
    <el-form :model="categoryForm" label-width="80px">
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
      <el-button @click="categoryDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmChangeCategory">确定</el-button>
    </template>
  </el-dialog>
  <el-dialog title="编辑备注" v-model="remarksDialogVisible" width="400px">
    <el-form :model="remarksForm" label-width="80px">
      <el-form-item label="备注">
        <el-input v-model="remarksForm.newRemarks" type="textarea" rows="3" placeholder="请输入备注信息"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="remarksDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmEditRemarks">确定</el-button>
    </template>
  </el-dialog>
  <!-- 批量分类对话框 -->
  <el-dialog title="批量更改文件分类" v-model="batchCategoryDialogVisible" width="400px">
    <el-form label-width="80px">
      <el-form-item label="选择分类">
        <el-select v-model="batchCategory" placeholder="请选择文件分类">
          <el-option label="证据材料" value="evidence" />
          <el-option label="诉讼文书" value="litigation" />
          <el-option label="裁判文书" value="judgment" />
          <el-option label="其他材料" value="others" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="batchCategoryDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmBatchCategory">确定</el-button>
    </template>
  </el-dialog>
  <!-- 批量备注对话框 -->
  <el-dialog title="批量添加备注" v-model="batchRemarksDialogVisible" width="400px">
    <el-form label-width="80px">
      <el-form-item label="备注内容">
        <el-input v-model="batchRemarks" type="textarea" rows="3" placeholder="请输入备注信息"></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="batchRemarksDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmBatchRemarks">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import axios from 'axios';
import { useUserStore } from '../stores/user';
import { getDocuments, uploadDocument, downloadDocument, deleteDocument, updateDocumentSort, batchDownloadDocuments, updateDocumentRemarks, batchUpdateRemarks, updateDocumentCategory } from '../api/document';
import Viewer from 'viewerjs';
import 'viewerjs/dist/viewer.css';
import * as pdfjsLib from 'pdfjs-dist';
import 'pdfjs-dist/web/pdf_viewer.css';
import { Upload, Download, Edit, Delete, Folder } from '@element-plus/icons-vue';
import DocumentUploader from '../components/document/DocumentUploader.vue';
import { saveAs } from 'file-saver';
import { PDFDocument } from 'pdf-lib';
import * as pdfjs from 'pdfjs-dist';

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
const renameForm = reactive({ newName: '', file: null });

// 更改分类对话框
const categoryDialogVisible = ref(false);
const categoryForm = reactive({ newCategory: '', file: null });

const pdfDialogVisible = ref(false);
const pdfUrl = ref('');
const imageDialogVisible = ref(false);
const imageUrl = ref('');
const unsupportedDialogVisible = ref(false);
const unsupportedFileName = ref('');

const treeData = ref([]);
const treeProps = { children: 'children', label: 'label' };

const treeRef = ref();
const selectedFiles = ref([]);
const batchCategoryDialogVisible = ref(false);
const batchCategory = ref('');
const batchRemarksDialogVisible = ref(false);
const batchRemarks = ref('');
const isDragging = ref(false);

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
      const response = await updateDocumentCategory(fileId, newCategory);
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
  },
  
  // 更新文件备注
  async updateFileRemarks(fileId, remarks) {
    try {
      const response = await updateDocumentRemarks(fileId, remarks);
      return response.data;
    } catch (error) {
      console.error('更新文件备注失败:', error);
      throw error;
    }
  },
  
  // 批量更新文件备注
  async batchUpdateFileRemarks(fileIds, remarks) {
    try {
      const response = await batchUpdateRemarks(fileIds, remarks);
      return response.data;
    } catch (error) {
      console.error('批量更新备注失败:', error);
      throw error;
    }
  },
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

// 重写handleUploadComplete方法
const handleUploadComplete = () => {
  uploadDialogVisible.value = false;
  loadDocuments();
  ElMessage({
    type: 'success',
    message: '文件上传成功'
  });
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
  renameForm.file = file;
  renameDialogVisible.value = true;
};

// 确认重命名
const confirmRename = async () => {
  if (!renameForm.newName) {
    ElMessage.warning('请输入新文件名');
    return;
  }
  
  try {
    await apiService.renameFile(renameForm.file.id, renameForm.newName);
    ElMessage.success('文件重命名成功');
    renameDialogVisible.value = false;
    fetchDocuments(); // 刷新文件列表
  } catch (error) {
    ElMessage.error('文件重命名失败');
  }
};

// 显示更改分类对话框
const showChangeCategoryDialog = (file) => {
  categoryForm.newCategory = file.category;
  categoryForm.file = file;
  categoryDialogVisible.value = true;
};

// 确认更改分类
const confirmChangeCategory = async () => {
  try {
    await apiService.changeFileCategory(categoryForm.file.id, categoryForm.newCategory);
    ElMessage.success('文件分类已更改');
    categoryDialogVisible.value = false;
    fetchDocuments(); // 刷新文件列表
  } catch (error) {
    ElMessage.error('更改文件分类失败');
  }
};

// 预览文件
const previewFile = (file) => {
  const fileType = file.fileType || '';
  if (fileType.startsWith('image/')) {
    imageUrl.value = `${baseApiUrl}/api/documents/${file.id}/preview`;
    imageDialogVisible.value = true;
  } else if (fileType === 'application/pdf') {
    pdfUrl.value = `${baseApiUrl}/api/documents/${file.id}/preview`;
    pdfDialogVisible.value = true;
  } else {
    unsupportedFileName.value = file.fileName;
    unsupportedDialogVisible.value = true;
  }
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

// 获取文档列表
const fetchDocuments = async () => {
  documentsLoading.value = true;
  try {
    const res = await getDocuments(caseId);
    documents.value = res.data;
    treeData.value = buildTreeData(res.data);
  } catch (error) {
    ElMessage.error('获取文件列表失败');
  } finally {
    documentsLoading.value = false;
  }
};

// 上传文档
const handleUpload = async (file) => {
  const formData = new FormData();
  formData.append('file', file.raw);
  formData.append('caseId', caseId);
  formData.append('category', uploadForm.category);
  formData.append('remarks', uploadForm.remarks);
  try {
    await uploadDocument(formData);
    ElMessage.success('上传成功');
    fetchDocuments();
  } catch (error) {
    ElMessage.error('上传失败');
  }
};

// 下载文档
const handleDownload = async (doc) => {
  try {
    const res = await downloadDocument(doc.id);
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', doc.fileName);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    ElMessage.error('下载失败');
  }
};

// 删除文档
const handleDelete = async (doc) => {
  try {
    await deleteDocument(doc.id);
    ElMessage.success('删除成功');
    fetchDocuments();
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

// 排序文档
const handleSort = async (doc, newOrder) => {
  try {
    await updateDocumentSort(doc.id, newOrder);
    ElMessage.success('排序已更新');
    fetchDocuments();
  } catch (error) {
    ElMessage.error('排序失败');
  }
};

const getPreviewUrl = (file) => `${baseApiUrl}/api/documents/${file.id}/preview`;

const handleTreeNodeClick = (data) => {
  if (data.fileType) {
    selectedFile.value = data;
  } else {
    selectedFile.value = null;
  }
};

// 文档列表转树形结构
const buildTreeData = (docs) => {
  const categories = [
    { key: 'evidence', label: '证据材料' },
    { key: 'litigation', label: '诉讼文书' },
    { key: 'judgment', label: '裁判文书' },
    { key: 'others', label: '其他材料' }
  ];
  const tree = categories.map(cat => ({
    id: cat.key,
    label: cat.label,
    children: []
  }));
  docs.forEach(doc => {
    const cat = tree.find(c => c.id === doc.category) || tree[tree.length - 1];
    cat.children.push({ ...doc, label: doc.fileName });
  });
  return tree;
};

const remarksDialogVisible = ref(false);
const remarksForm = reactive({ newRemarks: '', file: null });

const handleFileContextMenu = (cmd, file) => {
  if (cmd === 'rename') {
    renameForm.newName = file.fileName;
    renameForm.file = file;
    renameDialogVisible.value = true;
  } else if (cmd === 'changeCategory') {
    categoryForm.newCategory = file.category;
    categoryForm.file = file;
    categoryDialogVisible.value = true;
  } else if (cmd === 'editRemarks') {
    remarksForm.newRemarks = file.remarks || '';
    remarksForm.file = file;
    remarksDialogVisible.value = true;
  } else if (cmd === 'delete') {
    handleDelete(file);
  }
};

const confirmEditRemarks = async () => {
  try {
    await saveFileRemarks(remarksForm.file, remarksForm.newRemarks);
    ElMessage.success('备注已更新');
    remarksDialogVisible.value = false;
    fetchDocuments();
  } catch (error) {
    ElMessage.error('备注更新失败');
  }
};

const saveFileRemarks = async (file, newRemarks) => {
  // 这里假设有 apiService.updateFileRemarks 方法，实际可用 updateCase 或补充接口
  try {
    await apiService.updateFileRemarks(file.id, newRemarks ?? file.remarks);
  } catch {}
};

const handleTreeCheckChange = () => {
  // 获取所有选中的文件节点（排除分类节点）
  const checkedNodes = treeRef.value.getCheckedNodes(true);
  selectedFiles.value = checkedNodes.filter(n => n.fileType);
};

const handleBatchDelete = async () => {
  if (!selectedFiles.value.length) return;
  try {
    await ElMessageBox.confirm(`确定要删除选中的${selectedFiles.value.length}个文件吗？此操作不可逆！`, '批量删除', { type: 'warning' });
    for (const file of selectedFiles.value) {
      await deleteDocument(file.id);
    }
    ElMessage.success('批量删除成功');
    fetchDocuments();
  } catch {}
};

const openBatchCategoryDialog = () => {
  batchCategory.value = '';
  batchCategoryDialogVisible.value = true;
};

const confirmBatchCategory = async () => {
  if (!batchCategory.value) {
    ElMessage.warning('请选择分类');
    return;
  }
  try {
    for (const file of selectedFiles.value) {
      await apiService.changeFileCategory(file.id, batchCategory.value);
    }
    ElMessage.success('批量分类成功');
    batchCategoryDialogVisible.value = false;
    fetchDocuments();
  } catch {
    ElMessage.error('批量分类失败');
  }
};

// 处理拖拽结束事件
const handleDragEnd = async (draggingNode, dropNode, dropType, ev) => {
  // 只处理文件节点的拖拽，不处理分类节点
  if (!draggingNode.data.fileType) return;
  
  // 如果是拖到分类节点上，则更改文件分类
  if (dropNode && !dropNode.data.fileType && dropType === 'inner') {
    try {
      await apiService.changeFileCategory(draggingNode.data.id, dropNode.data.id);
      ElMessage.success('文件分类已更改');
      fetchDocuments();
    } catch (error) {
      ElMessage.error('更改文件分类失败');
    }
    return;
  }
  
  // 如果是在同一个分类内拖动排序
  if (dropNode && dropNode.data.fileType && dropNode.parent === draggingNode.parent) {
    const nodes = draggingNode.parent.childNodes;
    for (let i = 0; i < nodes.length; i++) {
      try {
        await updateDocumentSort(nodes[i].data.id, i);
      } catch (error) {
        console.error('更新排序失败:', error);
      }
    }
    ElMessage.success('文件排序已更新');
  }
};

// 批量下载
const handleBatchDownload = async () => {
  if (!selectedFiles.value.length) return;
  
  try {
    const ids = selectedFiles.value.map(file => file.id);
    const response = await batchDownloadDocuments(ids);
    
    const blob = new Blob([response.data]);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'documents.zip');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    ElMessage.success('批量下载开始');
  } catch (error) {
    ElMessage.error('批量下载失败');
  }
};

// 打开批量备注对话框
const openBatchRemarksDialog = () => {
  batchRemarks.value = '';
  batchRemarksDialogVisible.value = true;
};

// 确认批量备注
const confirmBatchRemarks = async () => {
  if (!batchRemarks.value) {
    ElMessage.warning('请输入备注内容');
    return;
  }
  
  try {
    const ids = selectedFiles.value.map(file => file.id);
    await apiService.batchUpdateFileRemarks(ids, batchRemarks.value);
    ElMessage.success('批量添加备注成功');
    batchRemarksDialogVisible.value = false;
    fetchDocuments();
  } catch (error) {
    ElMessage.error('批量添加备注失败');
  }
};

onMounted(() => {
  fetchCaseDetail();
  fetchDocuments();
});
</script>

<style scoped>
.case-detail-flex {
  display: flex;
  height: 85vh;
}

.tree-panel {
  width: 280px;
  border-right: 1px solid #eee;
  padding: 16px;
  background: #fafbfc;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.button-group {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.preview-panel {
  flex: 1;
  padding: 24px 32px;
  overflow: auto;
}

.tree-file-label {
  cursor: pointer;
  user-select: none;
  display: flex;
  align-items: center;
  padding: 4px 0;
}

.el-tree-node__content {
  height: 32px;
}

.el-button [class*='el-icon'] + span {
  margin-left: 4px;
}
</style> 