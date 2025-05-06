<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="login-title">系统登录</h2>
      <el-form :model="form" @submit.native.prevent="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onLogin" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>
      <div v-if="error" class="login-error">{{ error }}</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const form = ref({ username: '', password: '' })
const error = ref('')

const onLogin = async () => {
  error.value = ''
  try {
    const res = await axios.post('/api/auth/login', form.value)
    // 假设后端返回 token
    localStorage.setItem('token', res.data.token)
    router.push('/')
  } catch (e) {
    error.value = '用户名或密码错误'
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0e7ff 0%, #f8fafc 100%);
}
.login-card {
  width: 350px;
  padding: 32px 24px;
  border-radius: 16px;
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.1);
  text-align: center;
}
.login-title {
  font-size: 1.8rem;
  font-weight: 700;
  margin-bottom: 24px;
  color: #1e293b;
}
.login-error {
  color: #e53e3e;
  margin-top: 12px;
}
</style> 