import axios from 'axios';
import { createAsyncErrorHandler } from '../utils/errorHandler';

const api = axios.create({
  baseURL: 'http://localhost:8080/api'
});

// 添加请求拦截器处理认证
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// 基础API函数
export function getDocuments(caseId: number) {
  return api.get('/documents', { params: { caseId } });
}

export function uploadDocument(formData: FormData) {
  return api.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export function downloadDocument(id: number) {
  return api.get(`/documents/${id}/download`, { responseType: 'blob' });
}

export function deleteDocument(id: number) {
  return api.delete(`/documents/${id}`);
}

export function updateDocumentSort(id: number, sortOrder: number) {
  return api.put(`/documents/${id}/sort`, null, { params: { sortOrder } });
}

export function batchDownloadDocuments(ids: number[]) {
  return api.post('/documents/batch-download', { ids }, { responseType: 'blob' });
}

export function updateDocumentRemarks(id: number, remarks: string) {
  return api.put(`/documents/${id}/remarks`, { remarks });
}

export function batchUpdateRemarks(ids: number[], remarks: string) {
  return api.put('/documents/batch-remarks', { ids, remarks });
}

export function updateDocumentCategory(id: number, category: string) {
  return api.put(`/documents/${id}/category`, { category });
}

// 添加批量更新分类函数
export function batchUpdateCategory(ids: number[], category: string) {
  return api.put('/documents/batch-category', { ids, category });
}

// 添加文件预览功能
export function previewDocument(id: number) {
  return api.get(`/documents/${id}/preview`, { responseType: 'blob' });
}

// 添加批量删除功能
export function batchDeleteDocuments(ids: number[]) {
  return api.post('/documents/batch-delete', { ids });
}

// 分片上传相关API
export function initChunkedUpload(fileName: string, fileSize: number, fileType: string, caseId: number, category?: string, remarks?: string) {
  return api.post('/chunked-upload/init', {
    fileName,
    fileSize,
    fileType,
    caseId,
    category,
    remarks
  });
}

export function uploadChunk(taskId: string, chunkNumber: number, chunkFile: File) {
  const formData = new FormData();
  formData.append('taskId', taskId);
  formData.append('chunkNumber', chunkNumber.toString());
  formData.append('file', chunkFile);
  
  return api.post('/chunked-upload/chunk', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export function mergeChunks(taskId: string) {
  return api.post(`/chunked-upload/merge/${taskId}`);
}

export function getUploadTaskStatus(taskId: string) {
  return api.get(`/chunked-upload/status/${taskId}`);
}

export function cancelUpload(taskId: string) {
  return api.delete(`/chunked-upload/${taskId}`);
}

// 带错误处理的API调用（对外暴露使用这些函数）
export const safeGetDocuments = createAsyncErrorHandler(
  getDocuments, 
  '获取文档列表失败'
);

export const safeDownloadDocument = createAsyncErrorHandler(
  downloadDocument, 
  '下载文档失败'
);

export const safeDeleteDocument = createAsyncErrorHandler(
  deleteDocument, 
  '删除文档失败'
);

export const safeBatchDownloadDocuments = createAsyncErrorHandler(
  batchDownloadDocuments, 
  '批量下载文档失败'
);

export const safeUpdateDocumentRemarks = createAsyncErrorHandler(
  updateDocumentRemarks, 
  '更新文档备注失败'
);

export const safeBatchUpdateRemarks = createAsyncErrorHandler(
  batchUpdateRemarks, 
  '批量更新备注失败'
);

export const safeUpdateDocumentCategory = createAsyncErrorHandler(
  updateDocumentCategory, 
  '更新文档分类失败'
);

export const safeBatchUpdateCategory = createAsyncErrorHandler(
  batchUpdateCategory, 
  '批量更新分类失败'
);

export const safePreviewDocument = createAsyncErrorHandler(
  previewDocument, 
  '预览文档失败'
);

export const safeBatchDeleteDocuments = createAsyncErrorHandler(
  batchDeleteDocuments, 
  '批量删除文档失败'
);

// 分片上传带错误处理的API调用
export const safeInitChunkedUpload = createAsyncErrorHandler(
  initChunkedUpload,
  '初始化分片上传失败'
);

export const safeUploadChunk = createAsyncErrorHandler(
  uploadChunk,
  '上传分片失败'
);

export const safeMergeChunks = createAsyncErrorHandler(
  mergeChunks,
  '合并分片失败'
);

export const safeGetUploadTaskStatus = createAsyncErrorHandler(
  getUploadTaskStatus,
  '获取上传任务状态失败'
);

export const safeCancelUpload = createAsyncErrorHandler(
  cancelUpload,
  '取消上传任务失败'
); 