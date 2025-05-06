<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { Case as CaseType } from './api/case'
import type { Category } from './api/category'
import type { Tag } from './api/tag'
import { getCases, createCase, deleteCase, updateCase } from './api/case'
import { getCategories } from './api/category'
import { getTags } from './api/tag'

// 案件类型选项
const caseTypeOptions = ['民事', '刑事', '行政', '非诉']

// 不同类型对应的案由
const caseReasonMap = {
  民事: ['合同纠纷', '民间借贷', '婚姻家庭', '房产纠纷', '劳动争议', '交通事故', '知识产权', '其他'],
  刑事: ['盗窃', '诈骗', '故意伤害', '贩毒', '职务犯罪', '其他'],
  行政: ['行政处罚', '行政复议', '行政强制', '信息公开', '其他'],
  非诉: ['法律咨询', '合同审查', '尽职调查', '其他']
}
const statusOptions = ['归档中', '已归档']
const procedureOptions = ['一审', '二审', '再审', '执行']

const cases = ref<CaseType[]>([])
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
const form = ref<any>({
  caseType: '',
  caseNumber: '',
  caseReason: '',
  status: '',
  parties: '',
  agents: '',
  filingDate: '',
  court: '',
  procedure: '',
  categoryId: null,
  tagIds: []
})

// 动态案由选项
const caseReasonOptions = computed(() => {
  return caseReasonMap[form.value.caseType as keyof typeof caseReasonMap] || []
})

// 编辑相关
const editDialogVisible = ref(false)
const editForm = ref<any>({
  caseType: '',
  caseNumber: '',
  caseReason: '',
  status: '',
  parties: '',
  agents: '',
  filingDate: '',
  court: '',
  procedure: '',
  categoryId: null,
  tagIds: []
})
const editingId = ref<number | null>(null)
const editCaseReasonOptions = computed(() => {
  return caseReasonMap[editForm.value.caseType as keyof typeof caseReasonMap] || []
})

const loadCases = async () => {
  const res = await getCases()
  cases.value = res.data
}

const addCase = async () => {
  if (!form.value.caseNumber || !form.value.caseReason || !form.value.status) {
    ElMessage.warning('请填写完整信息')
    return
  }
  await createCase(form.value)
  ElMessage.success('添加成功')
  Object.assign(form.value, {
    caseType: '', caseNumber: '', caseReason: '', status: '', parties: '', agents: '', filingDate: '', court: '', procedure: '', categoryId: null, tagIds: []
  })
  await loadCases()
}

const removeCase = async (id: number) => {
  await deleteCase(id)
  ElMessage.success('删除成功')
  await loadCases()
}

const openEditDialog = (row: any) => {
  editingId.value = row.id || null
  editForm.value = { ...row }
  editDialogVisible.value = true
}

const saveEdit = async () => {
  if (editingId.value !== null) {
    await updateCase(editingId.value, editForm.value)
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    await loadCases()
  }
}

onMounted(async () => {
  await loadCases()
  categories.value = (await getCategories()).data
  tags.value = (await getTags()).data
})
</script>

<template>
  <div style="max-width: 1200px; margin: 40px auto;">
    <el-card>
      <h2 style="text-align:center;">案件列表</h2>
      <el-table :data="cases" style="width: 100%; margin-bottom: 20px;">
        <el-table-column prop="caseType" label="案件类型" width="100"/>
        <el-table-column prop="caseNumber" label="案号" width="120"/>
        <el-table-column prop="caseReason" label="案由" width="120"/>
        <el-table-column prop="status" label="状态" width="100"/>
        <el-table-column prop="parties" label="当事人" width="120"/>
        <el-table-column prop="agents" label="代理人" width="120"/>
        <el-table-column prop="filingDate" label="立案日期" width="120"/>
        <el-table-column prop="court" label="法院" width="120"/>
        <el-table-column prop="procedure" label="审理程序" width="120"/>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="removeCase(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-divider>新增案件</el-divider>
      <el-form :model="form" inline @submit.prevent="addCase">
        <el-form-item label="案件类型">
          <el-select v-model="form.caseType" placeholder="请选择类型" style="width: 100px;">
            <el-option v-for="item in caseTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="案号">
          <el-input v-model="form.caseNumber" placeholder="案号" />
        </el-form-item>
        <el-form-item label="案由">
          <el-select
            v-model="form.caseReason"
            placeholder="请选择或输入案由"
            style="width: 180px;"
            filterable
            allow-create
            default-first-option
          >
            <el-option v-for="item in caseReasonOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100px;">
            <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="当事人">
          <el-input v-model="form.parties" placeholder="当事人" />
        </el-form-item>
        <el-form-item label="代理人">
          <el-input v-model="form.agents" placeholder="代理人" />
        </el-form-item>
        <el-form-item label="立案日期">
          <el-date-picker v-model="form.filingDate" type="date" placeholder="选择日期" style="width: 140px;" value-format="YYYY-MM-DD"/>
        </el-form-item>
        <el-form-item label="法院">
          <el-input v-model="form.court" placeholder="法院" />
        </el-form-item>
        <el-form-item label="审理程序">
          <el-select v-model="form.procedure" placeholder="请选择程序" style="width: 100px;">
            <el-option v-for="item in procedureOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="form.categoryId"
            :data="categories"
            :props="{ label: 'name', children: 'children', value: 'id' }"
            placeholder="请选择分类"
            style="width: 200px"
            check-strictly
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tagIds" multiple filterable allow-create placeholder="请选择标签" style="width: 200px;">
            <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addCase">添加</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑案件" width="500px">
      <el-form :model="editForm">
        <el-form-item label="案件类型">
          <el-select v-model="editForm.caseType" placeholder="请选择类型" style="width: 100px;">
            <el-option v-for="item in caseTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="案号">
          <el-input v-model="editForm.caseNumber" />
        </el-form-item>
        <el-form-item label="案由">
          <el-select
            v-model="editForm.caseReason"
            placeholder="请选择或输入案由"
            style="width: 180px;"
            filterable
            allow-create
            default-first-option
          >
            <el-option v-for="item in editCaseReasonOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" placeholder="请选择状态" style="width: 100px;">
            <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="当事人">
          <el-input v-model="editForm.parties" />
        </el-form-item>
        <el-form-item label="代理人">
          <el-input v-model="editForm.agents" />
        </el-form-item>
        <el-form-item label="立案日期">
          <el-date-picker v-model="editForm.filingDate" type="date" placeholder="选择日期" style="width: 140px;" value-format="YYYY-MM-DD"/>
        </el-form-item>
        <el-form-item label="法院">
          <el-input v-model="editForm.court" />
        </el-form-item>
        <el-form-item label="审理程序">
          <el-select v-model="editForm.procedure" placeholder="请选择程序" style="width: 100px;">
            <el-option v-for="item in procedureOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="editForm.categoryId"
            :data="categories"
            :props="{ label: 'name', children: 'children', value: 'id' }"
            placeholder="请选择分类"
            style="width: 200px"
            check-strictly
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="editForm.tagIds" multiple filterable allow-create placeholder="请选择标签" style="width: 200px;">
            <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
