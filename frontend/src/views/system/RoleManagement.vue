<template>
  <div class="role-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleAdd">添加角色</el-button>
        </div>
      </template>

      <!-- 搜索区域 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="角色名称">
          <el-input v-model="searchKeyword" placeholder="输入角色名称搜索" clearable @clear="fetchRoleList" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchRoleList">搜索</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格区域 -->
      <el-table
        v-loading="loading"
        :data="roleList"
        border
        style="width: 100%"
      >
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="description" label="角色描述" />
        <el-table-column label="拥有权限">
          <template #default="scope">
            <div class="permission-tags-wrapper">
              <el-tag
                v-for="permission in scope.row.permissions"
                :key="permission.id"
                class="permission-tag"
                size="small"
              >
                {{ permission.name }}
              </el-tag>
              <span v-if="!scope.row.permissions || scope.row.permissions.length === 0" class="no-data">暂无权限</span>
              <el-button v-else link type="primary" @click="previewPermissions(scope.row)">查看更多</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              type="success"
              size="small"
              @click="handleAssignPermissions(scope.row)"
            >
              分配权限
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
              :disabled="scope.row.name === 'ADMIN'"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 角色表单对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      append-to-body
    >
      <el-form
        ref="roleFormRef"
        :model="roleForm"
        :rules="roleRules"
        label-width="100px"
      >
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" :disabled="dialogType === 'edit' && roleForm.name === 'ADMIN'" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 权限分配对话框 -->
    <el-dialog
      title="分配权限"
      v-model="permissionDialogVisible"
      width="700px"
      append-to-body
    >
      <div v-if="currentRole.name" class="role-info">
        <h3>角色：{{ currentRole.name }}</h3>
        <p>{{ currentRole.description }}</p>
      </div>
      
      <el-divider content-position="center">可分配权限</el-divider>

      <!-- 权限搜索 -->
      <div class="permission-search">
        <el-input
          v-model="permissionKeyword"
          placeholder="搜索权限"
          clearable
          prefix-icon="Search"
          @input="filterPermissionTree"
        >
        </el-input>
      </div>
      
      <div v-if="!permissionList.length" class="no-data-box">
        <el-empty description="暂无可分配权限" />
      </div>
      
      <div v-else class="permission-tree-container">
        <el-tree
          ref="permissionTreeRef"
          :data="filteredPermissionTree"
          :props="defaultProps"
          show-checkbox
          node-key="id"
          :default-checked-keys="selectedPermissionIds"
          :filter-node-method="filterNode"
          @check="handlePermissionCheck"
        >
          <template #default="{ node, data }">
            <span class="custom-tree-node">
              <span>{{ node.label }}</span>
              <span v-if="data.type" class="permission-type">{{ translatePermissionType(data.type) }}</span>
              <span v-if="data.description" class="permission-desc">{{ data.description }}</span>
            </span>
          </template>
        </el-tree>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="permissionDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitPermissionAssignment"
            :disabled="currentRole.name === 'ADMIN' && !isAdmin"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 权限预览对话框 -->
    <el-dialog
      title="权限详情"
      v-model="previewDialogVisible"
      width="700px"
      append-to-body
    >
      <div v-if="previewRole.name" class="role-info">
        <h3>角色：{{ previewRole.name }}</h3>
        <p>{{ previewRole.description }}</p>
      </div>

      <el-divider content-position="center">拥有的权限</el-divider>

      <el-tabs v-model="activePermissionTab">
        <el-tab-pane label="菜单权限" name="MENU">
          <el-empty v-if="!filteredPreviewPermissions.MENU.length" description="暂无菜单权限" />
          <div v-else class="permission-list">
            <el-tag
              v-for="permission in filteredPreviewPermissions.MENU"
              :key="permission.id"
              class="permission-tag-preview"
              type="success"
            >
              {{ permission.name }}
              <el-tooltip v-if="permission.description" effect="dark" :content="permission.description" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-tag>
          </div>
        </el-tab-pane>
        <el-tab-pane label="操作权限" name="OPERATION">
          <el-empty v-if="!filteredPreviewPermissions.OPERATION.length" description="暂无操作权限" />
          <div v-else class="permission-list">
            <el-tag
              v-for="permission in filteredPreviewPermissions.OPERATION"
              :key="permission.id"
              class="permission-tag-preview"
              type="warning"
            >
              {{ permission.name }}
              <el-tooltip v-if="permission.description" effect="dark" :content="permission.description" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-tag>
          </div>
        </el-tab-pane>
        <el-tab-pane label="API权限" name="API">
          <el-empty v-if="!filteredPreviewPermissions.API.length" description="暂无API权限" />
          <div v-else class="permission-list">
            <el-tag
              v-for="permission in filteredPreviewPermissions.API"
              :key="permission.id"
              class="permission-tag-preview"
              type="info"
            >
              {{ permission.name }}
              <el-tooltip v-if="permission.description" effect="dark" :content="permission.description" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-tag>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, getAllRoles, getRoleById, createRole, updateRole, deleteRole, getAllPermissions, assignPermissions } from '@/api/role'
import { InfoFilled } from '@element-plus/icons-vue'

// 数据区域
const roleList = ref([])
const permissionList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

// 对话框相关
const dialogVisible = ref(false)
const dialogType = ref('add')
const dialogTitle = ref('添加角色')
const roleFormRef = ref(null)
const roleForm = reactive({
  id: null,
  name: '',
  description: ''
})

// 表单校验规则
const roleRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入角色描述', trigger: 'blur' }
  ]
}

// 权限分配相关
const permissionDialogVisible = ref(false)
const permissionTreeRef = ref(null)
const currentRole = ref({})
const selectedPermissionIds = ref([])
const permissionKeyword = ref('')
const defaultProps = {
  children: 'children',
  label: 'name'
}

// 权限预览相关
const previewDialogVisible = ref(false)
const previewRole = ref({})
const activePermissionTab = ref('MENU')

// 权限树形结构数据
const permissionTree = computed(() => {
  // 分组权限：MENU-菜单权限，OPERATION-操作权限，API-接口权限
  const menuGroup = { id: 'menu-group', name: '菜单权限', children: [] }
  const operationGroup = { id: 'operation-group', name: '操作权限', children: [] }
  const apiGroup = { id: 'api-group', name: 'API权限', children: [] }
  
  permissionList.value.forEach(item => {
    const node = { ...item }
    
    switch (item.type) {
      case 'MENU':
        menuGroup.children.push(node)
        break
      case 'OPERATION':
        operationGroup.children.push(node)
        break
      case 'API':
        apiGroup.children.push(node)
        break
    }
  })
  
  const result = []
  if (menuGroup.children.length) result.push(menuGroup)
  if (operationGroup.children.length) result.push(operationGroup)
  if (apiGroup.children.length) result.push(apiGroup)
  
  return result
})

// 过滤后的权限树
const filteredPermissionTree = ref([])

// 当前登录用户是否是管理员
const isAdmin = computed(() => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return false
  
  const user = JSON.parse(userStr)
  return user.roles && user.roles.some(role => role.name === 'ADMIN')
})

// 预览权限列表（按类型分组）
const filteredPreviewPermissions = computed(() => {
  const permissions = previewRole.value.permissions || []
  return {
    'MENU': permissions.filter(p => p.type === 'MENU'),
    'OPERATION': permissions.filter(p => p.type === 'OPERATION'),
    'API': permissions.filter(p => p.type === 'API')
  }
})

// 监听权限关键词变化
watch(permissionKeyword, val => {
  filterPermissionTree()
})

// 生命周期
onMounted(() => {
  fetchRoleList()
  fetchPermissionList()
})

// 获取角色列表
const fetchRoleList = async () => {
  loading.value = true
  try {
    const response = await getRoleList(currentPage.value - 1, pageSize.value)
    roleList.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    console.error('获取角色列表失败', error)
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

// 获取权限列表
const fetchPermissionList = async () => {
  try {
    const response = await getAllPermissions()
    permissionList.value = response.data
    filteredPermissionTree.value = permissionTree.value
  } catch (error) {
    console.error('获取权限列表失败', error)
    ElMessage.error('获取权限列表失败')
  }
}

// 过滤权限树
const filterPermissionTree = () => {
  if (!permissionKeyword.value) {
    filteredPermissionTree.value = permissionTree.value
    return
  }
  
  // 深拷贝权限树
  const deepCopy = tree => {
    return tree.map(node => {
      const newNode = { ...node }
      if (newNode.children && newNode.children.length) {
        newNode.children = deepCopy(newNode.children)
      }
      return newNode
    })
  }
  
  // 过滤树节点
  const filterTree = (nodes, keyword) => {
    return nodes.filter(node => {
      if (node.children && node.children.length) {
        node.children = filterTree(node.children, keyword)
        return node.children.length > 0 || node.name.toLowerCase().includes(keyword.toLowerCase())
      }
      return node.name.toLowerCase().includes(keyword.toLowerCase()) || 
             (node.description && node.description.toLowerCase().includes(keyword.toLowerCase()))
    })
  }
  
  const copiedTree = deepCopy(permissionTree.value)
  filteredPermissionTree.value = filterTree(copiedTree, permissionKeyword.value)
}

// 过滤树节点方法（配合el-tree的filter-node-method使用）
const filterNode = (value, data) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase()) || 
         (data.description && data.description.toLowerCase().includes(value.toLowerCase()))
}

// 分页变化
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchRoleList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchRoleList()
}

// 添加角色
const handleAdd = () => {
  resetForm()
  dialogType.value = 'add'
  dialogTitle.value = '添加角色'
  dialogVisible.value = true
}

// 编辑角色
const handleEdit = (row) => {
  resetForm()
  dialogType.value = 'edit'
  dialogTitle.value = '编辑角色'
  Object.assign(roleForm, row)
  dialogVisible.value = true
}

// 删除角色
const handleDelete = (row) => {
  if (row.name === 'ADMIN') {
    ElMessage.warning('系统管理员角色不可删除')
    return
  }
  
  ElMessageBox.confirm(
    `确定要删除角色【${row.name}】吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        await deleteRole(row.id)
        ElMessage.success('删除成功')
        fetchRoleList()
      } catch (error) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {
      // 取消删除
    })
}

// 分配权限
const handleAssignPermissions = async (row) => {
  try {
    currentRole.value = row
    selectedPermissionIds.value = row.permissions 
      ? row.permissions.map(permission => permission.id) 
      : []
    permissionDialogVisible.value = true
    permissionKeyword.value = ''
    filteredPermissionTree.value = permissionTree.value
  } catch (error) {
    ElMessage.error('获取角色权限失败')
  }
}

// 权限分配树选择处理
const handlePermissionCheck = (data, checked) => {
  // 如果是管理员角色，且当前用户不是管理员，提示无权限
  if (currentRole.value.name === 'ADMIN' && !isAdmin.value) {
    ElMessage.warning('您没有权限修改管理员角色的权限')
    // 重置选择
    permissionTreeRef.value.setCheckedKeys(selectedPermissionIds.value)
    return
  }
}

// 预览权限
const previewPermissions = (role) => {
  previewRole.value = role
  activePermissionTab.value = 'MENU'
  previewDialogVisible.value = true
}

// 提交权限分配
const submitPermissionAssignment = async () => {
  if (currentRole.value.name === 'ADMIN' && !isAdmin.value) {
    ElMessage.warning('您没有权限修改管理员角色的权限')
    return
  }
  
  try {
    // 获取选中的节点ID（排除父节点）
    const checkedKeys = permissionTreeRef.value.getCheckedKeys(false)
    
    // 过滤掉非权限ID（如分组ID）
    const permissionIds = checkedKeys.filter(id => typeof id === 'number')
    
    await assignPermissions(currentRole.value.id, permissionIds)
    ElMessage.success('权限分配成功')
    permissionDialogVisible.value = false
    fetchRoleList()
  } catch (error) {
    ElMessage.error('权限分配失败')
  }
}

// 重置表单
const resetForm = () => {
  roleForm.id = null
  roleForm.name = ''
  roleForm.description = ''
  if (roleFormRef.value) {
    roleFormRef.value.resetFields()
  }
}

// 提交表单
const submitForm = async () => {
  if (roleFormRef.value) {
    await roleFormRef.value.validate(async (valid) => {
      if (valid) {
        try {
          if (dialogType.value === 'add') {
            await createRole(roleForm)
            ElMessage.success('角色创建成功')
          } else {
            await updateRole(roleForm.id, roleForm)
            ElMessage.success('角色更新成功')
          }
          dialogVisible.value = false
          fetchRoleList()
        } catch (error) {
          ElMessage.error(dialogType.value === 'add' ? '角色创建失败' : '角色更新失败')
        }
      }
    })
  }
}

// 转换权限类型显示
const translatePermissionType = (type) => {
  const types = {
    'MENU': '菜单',
    'OPERATION': '操作',
    'API': '接口'
  }
  return types[type] || type
}
</script>

<style scoped>
.role-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.permission-tag {
  margin: 0 5px 5px 0;
}

.permission-tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  max-height: 60px;
  overflow: hidden;
}

.no-data {
  color: #909399;
  font-size: 12px;
}

.role-info {
  margin-bottom: 20px;
}

.role-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
}

.role-info p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.permission-search {
  margin-bottom: 15px;
}

.permission-tree-container {
  max-height: 400px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.permission-type {
  margin-left: 8px;
  padding: 2px 4px;
  font-size: 12px;
  color: #409EFF;
  background-color: #ecf5ff;
  border-radius: 3px;
}

.permission-desc {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.no-data-box {
  padding: 20px;
  text-align: center;
}

.permission-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.permission-tag-preview {
  display: flex;
  align-items: center;
  margin-right: 8px;
  margin-bottom: 8px;
}

.info-icon {
  margin-left: 4px;
  font-size: 12px;
  cursor: pointer;
}
</style> 