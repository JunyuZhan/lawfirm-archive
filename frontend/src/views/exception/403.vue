<template>
  <div class="exception-page">
    <div class="exception-content">
      <div class="exception-image">
        <el-image :src="require('@/assets/images/403.svg')" alt="403" />
      </div>
      <h1 class="exception-title">403</h1>
      <div class="exception-desc">抱歉，您没有权限访问此页面</div>
      <div class="actions">
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.exception-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 80vh;
}

.exception-content {
  text-align: center;
}

.exception-image img {
  max-width: 300px;
  margin-bottom: 20px;
}

.exception-title {
  font-size: 72px;
  font-weight: 600;
  color: #303133;
  margin: 15px 0;
}

.exception-desc {
  font-size: 20px;
  color: #606266;
  margin-bottom: 30px;
}

.actions {
  margin-top: 30px;
}

.actions .el-button {
  margin: 0 10px;
}
</style> 