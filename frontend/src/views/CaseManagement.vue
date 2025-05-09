<template>
  <div class="case-management">
    <div class="page-header">
      <h1>案件管理</h1>
      <el-button type="primary" @click="showCreateDialog">新增案件</el-button>
    </div>
    
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索案件号、案由或当事人"
        prefix-icon="el-icon-search"
        clearable
        @input="handleSearch"
        style="width: 300px"
      />
      <div class="filters">
        <el-select v-model="filters.status" placeholder="案件状态" clearable @change="handleSearch">
          <el-option label="进行中" value="active" />
          <el-option label="已结案" value="closed" />
          <el-option label="已归档" value="archived" />
        </el-select>
        <el-select v-model="filters.type" placeholder="案件类型" clearable @change="handleSearch">
          <el-option label="民事" value="civil" />
          <el-option label="刑事" value="criminal" />
          <el-option label="行政" value="administrative" />
          <el-option label="非诉" value="non_litigation" />
        </el-select>
      </div>
    </div>
    
    <el-table :data="caseList" stripe style="width: 100%;" v-loading="loading">
      <el-table-column prop="caseNumber" label="案号" width="180" />
      <el-table-column prop="caseReason" label="案由" />
      <el-table-column prop="parties" label="当事人" />
      <el-table-column prop="filingDate" label="立案日期" width="120" />
      <el-table-column prop="court" label="管辖法院" width="180" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="viewCase(scope.row.id)">查看</el-button>
          <el-button size="small" type="primary" @click="editCase(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteCaseHandler(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="page"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      />
    </div>
    
    <!-- 新增/编辑案件弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="700px">
      <el-form :model="caseForm" label-width="100px" :rules="rules" ref="caseFormRef">
        <el-form-item label="案件类型" prop="type">
          <el-select v-model="caseForm.type" placeholder="请选择案件类型">
            <el-option label="民事" value="civil" />
            <el-option label="刑事" value="criminal" />
            <el-option label="行政" value="administrative" />
            <el-option label="非诉" value="non_litigation" />
          </el-select>
        </el-form-item>
        <el-form-item label="案号" prop="caseNumber">
          <el-input v-model="caseForm.caseNumber" placeholder="请输入案号" />
        </el-form-item>
        <el-form-item label="案由" prop="caseReason">
          <el-input v-model="caseForm.caseReason" placeholder="请输入案由" />
        </el-form-item>
        <el-form-item label="当事人" prop="parties">
          <el-input v-model="caseForm.parties" placeholder="请输入当事人" />
        </el-form-item>
        <el-form-item label="管辖法院" prop="court">
          <el-input v-model="caseForm.court" placeholder="请输入管辖法院" />
        </el-form-item>
        <el-form-item label="立案日期" prop="filingDate">
          <el-date-picker
            v-model="caseForm.filingDate"
            type="date"
            placeholder="选择立案日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="caseForm.status" placeholder="请选择状态">
            <el-option label="进行中" value="active" />
            <el-option label="已结案" value="closed" />
            <el-option label="已归档" value="archived" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remarks">
          <el-input 
            v-model="caseForm.remarks" 
            type="textarea" 
            rows="4"
            placeholder="请输入备注信息" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveCase">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getCases, createCase, updateCase, deleteCase } from '@/api/case';

const router = useRouter();
const loading = ref(false);
const caseList = ref([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKeyword = ref('');
const filters = reactive({
  status: '',
  type: ''
});

// 案件表单
const caseFormRef = ref(null);
const dialogVisible = ref(false);
const dialogTitle = ref('新增案件');
const caseForm = reactive({
  id: '',
  caseNumber: '',
  caseReason: '',
  parties: '',
  agents: '',
  filingDate: '',
  court: '',
  procedure: '',
  status: 'active',
  remarks: ''
});

const rules = {
  caseNumber: [{ required: true, message: '请输入案号', trigger: 'blur' }],
  caseReason: [{ required: true, message: '请输入案由', trigger: 'blur' }],
  parties: [{ required: true, message: '请输入当事人', trigger: 'blur' }],
  filingDate: [{ required: true, message: '请选择立案日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
};

// 状态映射
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

// 加载案件列表
const fetchCases = async () => {
  loading.value = true;
  try {
    const res = await getCases();
    caseList.value = res.data;
    total.value = res.data.length;
  } catch (error) {
    console.error('获取案件列表失败:', error);
    ElMessage.error('获取案件列表失败');
  } finally {
    loading.value = false;
  }
};

// 搜索处理
const handleSearch = () => {
  page.value = 1;
  fetchCases();
};

// 分页处理
const handleSizeChange = (newSize) => {
  pageSize.value = newSize;
  fetchCases();
};

const handleCurrentChange = (newPage) => {
  page.value = newPage;
  fetchCases();
};

// 查看案件详情
const viewCase = (id) => {
  router.push(`/cases/${id}`);
};

// 编辑案件
const editCase = (row) => {
  dialogTitle.value = '编辑案件';
  Object.assign(caseForm, row);
  dialogVisible.value = true;
};

// 删除案件
const deleteCaseHandler = (id) => {
  ElMessageBox.confirm('确定要删除此案件吗？此操作不可逆', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteCase(id);
      ElMessage.success('删除成功');
      fetchCases();
    } catch (error) {
      console.error('删除案件失败:', error);
      ElMessage.error('删除案件失败');
    }
  }).catch(() => {});
};

// 显示新增对话框
const showCreateDialog = () => {
  dialogTitle.value = '新增案件';
  Object.keys(caseForm).forEach(key => {
    caseForm[key] = key === 'status' ? 'active' : '';
  });
  dialogVisible.value = true;
};

// 保存案件
const saveCase = () => {
  caseFormRef.value.validate(async (valid) => {
    if (!valid) return;
    try {
      if (caseForm.id) {
        await updateCase(caseForm.id, caseForm);
        ElMessage.success('编辑成功');
      } else {
        await createCase(caseForm);
        ElMessage.success('新增成功');
      }
      dialogVisible.value = false;
      fetchCases();
    } catch (error) {
      console.error('保存案件失败:', error);
      ElMessage.error('保存案件失败');
    }
  });
};

onMounted(() => {
  fetchCases();
});
</script>

<style scoped>
.case-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
  color: #333;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.filters {
  display: flex;
  gap: 10px;
  margin-left: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.el-tag {
  border-radius: 4px;
}
</style> 