<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategories } from '../api/category'
import axios from 'axios'

interface Category {
  id: number
  name: string
  parent?: Category
  children?: Category[]
}

const categories = ref<Category[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref<{ id?: number; name: string; parentId?: number | null }>({
  name: '',
  parentId: null
})
const currentParentId = ref<number | null>(null)

const loadCategories = async () => {
  categories.value = (await getCategories()).data
}

const openAdd = (parentId: number | null = null) => {
  isEdit.value = false
  form.value = { name: '', parentId }
  dialogVisible.value = true
}

const openEdit = (cat: Category) => {
  isEdit.value = true
  form.value = { id: cat.id, name: cat.name, parentId: cat.parent?.id ?? null }
  dialogVisible.value = true
}

const saveCategory = async () => {
  if (!form.value.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (isEdit.value && form.value.id) {
    await axios.put(`/api/categories/${form.value.id}`, {
      name: form.value.name,
      parent: form.value.parentId ? { id: form.value.parentId } : null
    })
    ElMessage.success('修改成功')
  } else {
    await axios.post('/api/categories', {
      name: form.value.name,
      parent: form.value.parentId ? { id: form.value.parentId } : null
    })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  await loadCategories()
}

const removeCategory = async (id: number) => {
  await axios.delete(`/api/categories/${id}`)
  ElMessage.success('删除成功')
  await loadCategories()
}

onMounted(loadCategories)
</script>

<template>
  <div style="max-width: 600px; margin: 40px auto;">
    <el-card>
      <h2 style="text-align:center;">分类管理</h2>
      <el-button type="primary" @click="openAdd(null)" style="margin-bottom: 16px;">新增顶级分类</el-button>
      <el-tree
        :data="categories"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        default-expand-all
      >
        <template #default="{ node, data }">
          <span>{{ data.name }}</span>
          <el-button size="small" @click.stop="openAdd(data.id)">新增子分类</el-button>
          <el-button size="small" @click.stop="openEdit(data)">编辑</el-button>
          <el-button size="small" type="danger" @click.stop="removeCategory(data.id)">删除</el-button>
        </template>
      </el-tree>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'">
      <el-form :model="form">
        <el-form-item label="分类名称">
          <el-input v-model="form.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
