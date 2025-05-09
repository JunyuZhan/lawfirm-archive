<template>
  <div class="user-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="action-buttons">
            <el-upload
              class="upload-demo"
              action=""
              :http-request="handleImportUsers"
              :show-file-list="false"
              accept=".xlsx,.xls,.csv"
            >
              <el-button type="primary" :icon="Upload">批量导入</el-button>
            </el-upload>
            <el-button type="success" :icon="Download" @click="handleExportUsers">批量导出</el-button>
            <el-button type="primary" @click="handleAdd">添加用户</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索区域 -->
      <div class="search-area">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索用户名/姓名/邮箱"
              class="search-input"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :span="4">
            <el-select v-model="searchStatus" placeholder="状态筛选" clearable @change="handleSearch">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="searchRole" placeholder="角色筛选" clearable @change="handleSearch">
              <el-option v-for="role in roleList" :key="role.id" :label="role.name" :value="role.id" />
            </el-select>
          </el-col>
          <el-col :span="4" :offset="6">
            <el-button-group>
              <el-button type="success" @click="handleBatchEnable">批量启用</el-button>
              <el-button type="danger" @click="handleBatchDisable">批量禁用</el-button>
            </el-button-group>
          </el-col>
        </el-row>
      </div>

      <!-- 表格区域 -->
      <el-table
        v-loading="loading"
        :data="userList"
        border
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="department" label="部门" />
        <el-table-column prop="position" label="职位" />
        <el-table-column label="角色">
          <template #default="scope">
            <el-tag
              v-for="role in scope.row.roles"
              :key="role.id"
              class="role-tag"
            >
              {{ role.name }}
            </el-tag>
            <span v-if="!scope.row.roles || scope.row.roles.length === 0" class="no-role">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val) => handleStatusChange(scope.row, val)"
              :disabled="scope.row.username === 'admin'"
            />
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
              @click="handleAssignRoles(scope.row)"
            >
              分配角色
            </el-button>
            <el-button
              type="warning"
              size="small"
              @click="handleResetPassword(scope.row)"
              :disabled="scope.row.username === 'admin'"
            >
              重置密码
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row)"
              :disabled="scope.row.username === 'admin'"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
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

    <!-- 用户表单对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      append-to-body
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="dialogType === 'edit'" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'add'" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="userForm.department" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="userForm.position" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="userForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 角色分配对话框 -->
    <el-dialog
      title="分配角色"
      v-model="roleDialogVisible"
      width="500px"
      append-to-body
    >
      <el-form label-width="100px">
        <el-form-item label="用户">
          <el-input v-model="currentUser.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="selectedRoleIds">
            <el-checkbox
              v-for="role in roleList"
              :key="role.id"
              :label="role.id"
              :disabled="role.name === 'ADMIN' && currentUser.username !== 'admin'"
            >
              {{ role.name }} ({{ role.description }})
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRoleAssignment">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入模板对话框 -->
    <el-dialog
      title="用户导入说明"
      v-model="importDialogVisible"
      width="600px"
    >
      <div class="import-guide">
        <h3>导入说明</h3>
        <p>1. 请下载导入模板，按模板格式填写用户数据</p>
        <p>2. 支持的文件格式：Excel(.xlsx, .xls)和CSV(.csv)</p>
        <p>3. 必填字段：用户名、密码、邮箱、姓名</p>
        <p>4. 状态字段：1表示启用，0表示禁用，默认为1</p>
        <p>5. 批量导入上限为200条数据</p>
      </div>
      <div class="import-template">
        <el-button type="primary" @click="downloadTemplate">下载导入模板</el-button>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialogVisible = false">关闭</el-button>
          <el-upload
            class="upload-demo"
            action=""
            :http-request="handleImportUsers"
            :show-file-list="false"
            accept=".xlsx,.xls,.csv"
          >
            <el-button type="success">选择文件导入</el-button>
          </el-upload>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, Download } from '@element-plus/icons-vue'
import { getUserList, createUser, updateUser, deleteUser, resetUserPassword, assignRoles, updateUserStatus, importUsers, exportUsers } from '@/api/user'
import { getAllRoles } from '@/api/role'
import * as XLSX from 'xlsx'

// 数据区域
const userList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const searchStatus = ref('')
const searchRole = ref('')
const selectedUsers = ref([])
const importDialogVisible = ref(false)

// 对话框相关
const dialogVisible = ref(false)
const dialogType = ref('add')
const dialogTitle = ref('添加用户')
const userFormRef = ref(null)
const userForm = reactive({
  id: null,
  username: '',
  password: '',
  email: '',
  realName: '',
  phone: '',
  department: '',
  position: '',
  status: 1
})

// 表单校验规则
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ]
}

// 角色相关
const roleDialogVisible = ref(false)
const roleList = ref([])
const selectedRoleIds = ref([])
const currentUser = ref({})

// 生命周期
onMounted(() => {
  fetchUserList()
  fetchRoleList()
})

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const response = await getUserList(searchKeyword.value, currentPage.value - 1, pageSize.value)
    userList.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    console.error('获取用户列表失败', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 获取所有角色
const fetchRoleList = async () => {
  try {
    const response = await getAllRoles()
    roleList.value = response.data
  } catch (error) {
    console.error('获取角色列表失败', error)
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchUserList()
}

// 分页变化
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchUserList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchUserList()
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

// 状态变化
const handleStatusChange = async (user, status) => {
  if (user.username === 'admin') {
    ElMessage.warning('管理员账号状态不可修改')
    user.status = 1 // 重置为启用状态
    return
  }
  
  try {
    await updateUserStatus(user.id, status)
    ElMessage.success(`用户【${user.username}】${status === 1 ? '启用' : '禁用'}成功`)
  } catch (error) {
    user.status = status === 1 ? 0 : 1 // 操作失败，恢复状态
    ElMessage.error(`操作失败`)
  }
}

// 批量启用
const handleBatchEnable = async () => {
  if (!selectedUsers.value.length) {
    ElMessage.warning('请选择要启用的用户')
    return
  }
  
  // 过滤掉admin用户
  const filteredUsers = selectedUsers.value.filter(user => user.username !== 'admin')
  
  if (filteredUsers.length === 0) {
    ElMessage.warning('所选用户中不包含可操作的用户')
    return
  }
  
  try {
    await Promise.all(filteredUsers.map(user => updateUserStatus(user.id, 1)))
    ElMessage.success(`已成功启用 ${filteredUsers.length} 个用户`)
    fetchUserList()
  } catch (error) {
    ElMessage.error('批量启用失败')
  }
}

// 批量禁用
const handleBatchDisable = async () => {
  if (!selectedUsers.value.length) {
    ElMessage.warning('请选择要禁用的用户')
    return
  }
  
  // 过滤掉admin用户
  const filteredUsers = selectedUsers.value.filter(user => user.username !== 'admin')
  
  if (filteredUsers.length === 0) {
    ElMessage.warning('所选用户中不包含可操作的用户')
    return
  }
  
  try {
    await Promise.all(filteredUsers.map(user => updateUserStatus(user.id, 0)))
    ElMessage.success(`已成功禁用 ${filteredUsers.length} 个用户`)
    fetchUserList()
  } catch (error) {
    ElMessage.error('批量禁用失败')
  }
}

// 添加用户
const handleAdd = () => {
  resetForm()
  dialogType.value = 'add'
  dialogTitle.value = '添加用户'
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  resetForm()
  dialogType.value = 'edit'
  dialogTitle.value = '编辑用户'
  userForm.id = row.id
  userForm.username = row.username
  userForm.email = row.email
  userForm.realName = row.realName
  userForm.phone = row.phone
  userForm.department = row.department
  userForm.position = row.position
  userForm.status = row.status
  dialogVisible.value = true
}

// 删除用户
const handleDelete = (row) => {
  if (row.username === 'admin') {
    ElMessage.warning('管理员账号不可删除')
    return
  }
  
  ElMessageBox.confirm(
    `确定要删除用户【${row.username}】吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        await deleteUser(row.id)
        ElMessage.success('删除成功')
        fetchUserList()
      } catch (error) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {
      // 取消删除
    })
}

// 分配角色
const handleAssignRoles = (row) => {
  currentUser.value = row
  selectedRoleIds.value = row.roles ? row.roles.map(role => role.id) : []
  roleDialogVisible.value = true
}

// 重置密码
const handleResetPassword = (row) => {
  if (row.username === 'admin') {
    ElMessage.warning('管理员账号密码不可通过此方式重置')
    return
  }
  
  ElMessageBox.confirm(
    `确定要重置用户【${row.username}】的密码吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        await resetUserPassword(row.id)
        ElMessage.success('密码重置成功，新密码已发送至用户邮箱')
      } catch (error) {
        ElMessage.error('密码重置失败')
      }
    })
    .catch(() => {
      // 取消操作
    })
}

// 提交表单
const submitForm = async () => {
  if (userFormRef.value) {
    await userFormRef.value.validate(async (valid) => {
      if (valid) {
        try {
          if (dialogType.value === 'add') {
            await createUser(userForm)
            ElMessage.success('用户创建成功')
          } else {
            await updateUser(userForm.id, userForm)
            ElMessage.success('用户更新成功')
          }
          dialogVisible.value = false
          fetchUserList()
        } catch (error) {
          ElMessage.error(dialogType.value === 'add' ? '用户创建失败' : '用户更新失败')
        }
      }
    })
  }
}

// 提交角色分配
const submitRoleAssignment = async () => {
  try {
    await assignRoles(currentUser.value.id, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    fetchUserList()
  } catch (error) {
    ElMessage.error('角色分配失败')
  }
}

// 重置表单
const resetForm = () => {
  userForm.id = null
  userForm.username = ''
  userForm.password = ''
  userForm.email = ''
  userForm.realName = ''
  userForm.phone = ''
  userForm.department = ''
  userForm.position = ''
  userForm.status = 1
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
}

// 显示导入对话框
const showImportDialog = () => {
  importDialogVisible.value = true
}

// 下载导入模板
const downloadTemplate = () => {
  const templateData = [
    {
      '用户名': 'example',
      '密码': 'password123',
      '姓名': '张三',
      '邮箱': 'example@example.com',
      '手机号': '13800138000',
      '部门': '法务部',
      '职位': '律师',
      '状态': 1
    }
  ]
  
  const ws = XLSX.utils.json_to_sheet(templateData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '用户导入模板')
  XLSX.writeFile(wb, '用户导入模板.xlsx')
}

// 处理用户导入
const handleImportUsers = async ({ file }) => {
  importDialogVisible.value = false
  loading.value = true
  
  try {
    // 读取Excel文件
    const reader = new FileReader()
    reader.onload = async (e) => {
      try {
        const data = new Uint8Array(e.target.result)
        const workbook = XLSX.read(data, { type: 'array' })
        
        // 获取第一个工作表
        const worksheet = workbook.Sheets[workbook.SheetNames[0]]
        // 转换为JSON
        const jsonData = XLSX.utils.sheet_to_json(worksheet)
        
        if (jsonData.length > 200) {
          ElMessage.error('导入数据不能超过200条')
          loading.value = false
          return
        }
        
        // 转换字段名称
        const users = jsonData.map(item => ({
          username: item['用户名'],
          password: item['密码'],
          realName: item['姓名'],
          email: item['邮箱'],
          phone: item['手机号'],
          department: item['部门'],
          position: item['职位'],
          status: item['状态'] !== undefined ? item['状态'] : 1
        }))
        
        // 发送到后端
        const response = await importUsers(users)
        
        ElMessage.success(`成功导入 ${response.data.successCount} 条数据，失败 ${response.data.failCount} 条`)
        fetchUserList()
      } catch (error) {
        ElMessage.error('解析文件失败，请确保文件格式正确')
      } finally {
        loading.value = false
      }
    }
    
    reader.readAsArrayBuffer(file)
  } catch (error) {
    loading.value = false
    ElMessage.error('导入失败')
  }
}

// 导出用户数据
const handleExportUsers = async () => {
  try {
    loading.value = true
    
    // 调用后端导出接口
    const response = await exportUsers()
    
    // 将后端返回的数据转换为Excel
    const users = response.data.map(user => ({
      '用户名': user.username,
      '姓名': user.realName,
      '邮箱': user.email,
      '手机号': user.phone,
      '部门': user.department,
      '职位': user.position,
      '状态': user.status === 1 ? '启用' : '禁用',
      '角色': user.roles ? user.roles.map(role => role.name).join(', ') : '',
      '创建时间': user.createdAt,
      '最后登录': user.lastLoginTime
    }))
    
    // 创建工作簿和工作表
    const ws = XLSX.utils.json_to_sheet(users)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '用户数据')
    
    // 导出文件
    XLSX.writeFile(wb, `用户数据_${new Date().toISOString().split('T')[0]}.xlsx`)
    
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.search-area {
  margin-bottom: 20px;
}

.search-input {
  width: 100%;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.role-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

.no-role {
  color: #909399;
  font-size: 12px;
}

.import-guide {
  margin-bottom: 20px;
}

.import-guide h3 {
  margin-top: 0;
  margin-bottom: 10px;
}

.import-guide p {
  margin: 5px 0;
  line-height: 1.5;
}

.import-template {
  margin: 20px 0;
  text-align: center;
}
</style> 