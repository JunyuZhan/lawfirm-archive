<template>
  <div class="login-layout">
    <!-- 左侧插画区 -->
    <div class="login-left">
      <div class="login-illustration">
        <!-- SVG插画占位，可替换为自己的图片 -->
        <svg width="340" height="240" viewBox="0 0 340 240" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="340" height="240" rx="32" fill="#3B82F6"/>
          <circle cx="90" cy="120" r="48" fill="#fff" fill-opacity="0.18"/>
          <rect x="160" y="60" width="120" height="24" rx="8" fill="#fff" fill-opacity="0.3"/>
          <rect x="160" y="100" width="80" height="16" rx="6" fill="#fff" fill-opacity="0.3"/>
          <rect x="160" y="130" width="100" height="16" rx="6" fill="#fff" fill-opacity="0.3"/>
          <rect x="160" y="160" width="60" height="16" rx="6" fill="#fff" fill-opacity="0.3"/>
        </svg>
      </div>
    </div>
    <!-- 右侧登录区 -->
    <div class="login-right">
      <div class="login-card">
        <div class="system-title">律师事务所案件档案管理系统</div>
        <el-form :model="form" @submit.prevent="onLogin" class="login-form">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" clearable prefix-icon="el-icon-user" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" clearable prefix-icon="el-icon-lock" show-password />
          </el-form-item>
          <el-form-item class="login-options">
            <el-checkbox v-model="form.remember">记住密码</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" size="large" style="width:100%;font-size:18px;" :loading="loading">登录</el-button>
          </el-form-item>
        </el-form>
        <div v-if="error" class="login-error">{{ error }}</div>
        <div class="login-footer">© 2024 律师事务所案件档案管理系统</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { getCurrentUser } from '@/api/user'

const router = useRouter()
const route = useRoute()
const form = ref({ username: '', password: '', remember: false })
const loading = ref(false)
const error = ref('')

const onLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  error.value = ''
  loading.value = true
  try {
    // 登录获取token
    const res = await login({
      username: form.value.username,
      password: form.value.password
    })
    
    // 保存token
    localStorage.setItem('token', res.data.token)
    
    // 获取用户信息
    const userRes = await getCurrentUser()
    localStorage.setItem('user', JSON.stringify(userRes.data))
    
    ElMessage.success('登录成功')
    
    // 如果有重定向，则跳转到重定向地址
    const redirect = route.query.redirect || ''
    await router.push(redirect || '/cases')
  } catch (e) {
    error.value = '用户名或密码错误'
    ElMessage.error('登录失败：' + (e.response?.data?.message || '用户名或密码错误'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
html, body {
  height: 100%;
  margin: 0;
  padding: 0;
}
.login-layout {
  display: flex;
  min-height: 100vh;
  width: 100vw;
}
.login-left {
  flex: 1.2;
  background: linear-gradient(120deg, #2563eb 0%, #60a5fa 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-illustration {
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-right {
  flex: 1;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  width: 370px;
  padding: 44px 36px 32px 36px;
  border-radius: 18px;
  box-shadow: 0 4px 32px 0 rgba(31, 38, 135, 0.10);
  background: #fff;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.system-title {
  font-size: 1.6rem;
  font-weight: 800;
  margin-bottom: 36px;
  color: #1e293b;
  letter-spacing: 1px;
}
.login-form {
  width: 100%;
  margin-bottom: 8px;
}
.login-options {
  justify-content: flex-start;
  margin-bottom: -10px;
}
.login-error {
  color: #e53e3e;
  margin-top: 10px;
  font-size: 15px;
}
.login-footer {
  margin-top: 32px;
  color: #b0b3b8;
  font-size: 13px;
  text-align: center;
  width: 100%;
}
@media (max-width: 900px) {
  .login-layout {
    flex-direction: column;
  }
  .login-left {
    min-height: 180px;
    flex: none;
    width: 100vw;
    justify-content: center;
  }
  .login-right {
    flex: none;
    width: 100vw;
    justify-content: center;
  }
  .login-card {
    margin: 32px 0;
  }
}
@media (max-width: 500px) {
  .login-card {
    width: 95vw;
    padding: 18px 2vw 12px 2vw;
  }
  .system-title {
    font-size: 1.1rem;
    margin-bottom: 18px;
  }
}
</style> 