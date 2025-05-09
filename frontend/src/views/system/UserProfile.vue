<template>
  <div class="user-profile">
    <el-row :gutter="20">
      <!-- 左侧个人信息卡片 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="6" :xl="6">
        <el-card class="profile-card">
          <div class="user-avatar">
            <el-avatar
              :size="100"
              :src="userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
            />
          </div>
          <div class="user-info">
            <h2>{{ userInfo.realName || userInfo.username }}</h2>
            <p>{{ userInfo.email }}</p>
            <p>{{ userInfo.position || '未设置职位' }}</p>
            <p>{{ userInfo.department || '未设置部门' }}</p>
            <div class="user-roles">
              <el-tag
                v-for="role in userInfo.roles"
                :key="role.id"
                class="role-tag"
              >
                {{ role.name }}
              </el-tag>
            </div>
            <p class="last-login" v-if="userInfo.lastLoginTime">
              最近登录: {{ formatDateTime(userInfo.lastLoginTime) }}
            </p>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧内容区 -->
      <el-col :xs="24" :sm="24" :md="16" :lg="18" :xl="18">
        <el-tabs v-model="activeTab">
          <!-- 基本信息标签页 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>基本信息</span>
                </div>
              </template>
              <el-form
                ref="userFormRef"
                :model="userForm"
                :rules="userRules"
                label-width="100px"
              >
                <el-form-item label="用户名">
                  <el-input v-model="userForm.username" disabled />
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
                <el-form-item>
                  <el-button type="primary" @click="submitUserForm">保存</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-tab-pane>

          <!-- 修改密码标签页 -->
          <el-tab-pane label="修改密码" name="password">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>修改密码</span>
                </div>
              </template>
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="100px"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="passwordForm.newPassword"
                    type="password"
                    show-password
                  />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    show-password
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitPasswordForm">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentUser, updateUser, changePassword } from '@/api/user'

// 数据区域
const userInfo = ref({})
const activeTab = ref('basic')
const userFormRef = ref(null)
const passwordFormRef = ref(null)

// 个人信息表单
const userForm = reactive({
  id: null,
  username: '',
  email: '',
  realName: '',
  phone: '',
  department: '',
  position: ''
})

// 个人信息表单校验规则
const userRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ]
}

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 自定义验证新密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 密码表单校验规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在6-20个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 生命周期
onMounted(() => {
  fetchUserInfo()
})

// 获取当前用户信息
const fetchUserInfo = async () => {
  try {
    const userRes = await getCurrentUser()
    userInfo.value = userRes.data
    
    // 填充个人信息表单
    userForm.id = userInfo.value.id
    userForm.username = userInfo.value.username
    userForm.email = userInfo.value.email
    userForm.realName = userInfo.value.realName
    userForm.phone = userInfo.value.phone
    userForm.department = userInfo.value.department
    userForm.position = userInfo.value.position
  } catch (error) {
    ElMessage.error('获取用户信息失败')
  }
}

// 提交个人信息表单
const submitUserForm = async () => {
  if (userFormRef.value) {
    await userFormRef.value.validate(async (valid) => {
      if (valid) {
        try {
          await updateUser(userForm.id, userForm)
          ElMessage.success('保存成功')
          // 更新本地存储的用户信息
          fetchUserInfo()
          localStorage.setItem('user', JSON.stringify({
            ...JSON.parse(localStorage.getItem('user')),
            ...userForm
          }))
        } catch (error) {
          ElMessage.error('保存失败')
        }
      }
    })
  }
}

// 提交密码表单
const submitPasswordForm = async () => {
  if (passwordFormRef.value) {
    await passwordFormRef.value.validate(async (valid) => {
      if (valid) {
        try {
          await changePassword(
            userInfo.value.id,
            passwordForm.oldPassword,
            passwordForm.newPassword
          )
          ElMessage.success('密码修改成功')
          // 清空表单
          passwordForm.oldPassword = ''
          passwordForm.newPassword = ''
          passwordForm.confirmPassword = ''
          passwordFormRef.value.resetFields()
        } catch (error) {
          ElMessage.error('密码修改失败，请检查原密码是否正确')
        }
      }
    })
  }
}

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  const date = new Date(dateTimeStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}
</script>

<style scoped>
.user-profile {
  padding: 20px;
}

.profile-card {
  text-align: center;
  margin-bottom: 20px;
}

.user-avatar {
  margin: 20px 0;
}

.user-info {
  padding: 0 15px 20px;
}

.user-info h2 {
  margin: 10px 0;
  font-size: 1.5rem;
}

.user-info p {
  margin: 5px 0;
  color: #606266;
  font-size: 0.9rem;
}

.user-roles {
  margin: 15px 0;
}

.role-tag {
  margin: 0 5px 5px 0;
}

.last-login {
  font-size: 0.8rem;
  color: #909399;
  margin-top: 15px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 