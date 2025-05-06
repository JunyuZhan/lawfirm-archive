<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>律师事务所案件档案管理系统</h1>
      </div>
      <div class="login-form-container">
        <div class="system-icon">
          <svg viewBox="0 0 24 24" width="48" height="48">
            <path fill="currentColor" d="M12,21L8.5,16H10V8H5V12L1,8L5,4V8H10V3H14V8H19V4L23,8L19,12V8H14V16H15.5L12,21Z" />
          </svg>
        </div>
        <div class="system-title">
          <h2>系统登录</h2>
        </div>
        <form @submit.prevent="onLogin">
          <div class="input-group">
            <label>用户名</label>
            <input 
              type="text" 
              v-model="form.username" 
              placeholder="请输入用户名" 
              autocomplete="username" 
              required
            />
          </div>
          <div class="input-group">
            <label>密码</label>
            <input 
              type="password" 
              v-model="form.password" 
              placeholder="请输入密码" 
              autocomplete="current-password" 
              required
            />
          </div>
          <div class="login-options">
            <label class="remember-me">
              <input type="checkbox" /> 记住我
            </label>
            <a href="#" class="forgot-password">忘记密码?</a>
          </div>
          <div class="btn-group">
            <button type="submit">登录</button>
          </div>
          <div v-if="error" class="error-message">{{ error }}</div>
        </form>
      </div>
    </div>
    <div class="login-footer">
      <p>© 2025 律师事务所案件档案管理系统 版权所有</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const form = ref({ username: '', password: '' })
const error = ref('')

const onLogin = async () => {
  error.value = ''
  try {
    const res = await axios.post('/api/auth/login', form.value)
    localStorage.setItem('token', res.data.token)
    window.location.reload()
  } catch (e) {
    error.value = '用户名或密码错误'
  }
}
</script>

<style>
/* 全局样式重置 */
html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow-x: hidden;
}
</style>

<style scoped>
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-card {
  width: 480px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
  overflow: hidden;
  margin: 0 20px;
}

.login-header {
  background: #1e40af;
  color: white;
  padding: 25px 30px;
  text-align: center;
}

.login-header h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.login-form-container {
  padding: 30px;
}

.system-icon {
  text-align: center;
  color: #1e40af;
  margin-bottom: 8px;
}

.system-title {
  text-align: center;
  margin-bottom: 24px;
}

.system-title h2 {
  font-size: 1.5rem;
  color: #1e3a8a;
  margin: 0;
  font-weight: 600;
}

.input-group {
  margin-bottom: 20px;
}

.input-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #4b5563;
  font-weight: 500;
}

.input-group input {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.input-group input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.login-options {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  font-size: 14px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #4b5563;
  cursor: pointer;
}

.forgot-password {
  color: #1e40af;
  text-decoration: none;
}

.forgot-password:hover {
  text-decoration: underline;
}

.btn-group {
  margin-top: 5px;
}

.btn-group button {
  width: 100%;
  padding: 14px;
  background: #1e40af;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-group button:hover {
  background: #1e3a8a;
  transform: translateY(-1px);
}

.btn-group button:active {
  transform: translateY(1px);
}

.error-message {
  color: #dc2626;
  margin-top: 15px;
  text-align: center;
}

.login-footer {
  margin-top: 40px;
  text-align: center;
  color: rgba(75, 85, 99, 0.8);
  font-size: 13px;
}
</style>
