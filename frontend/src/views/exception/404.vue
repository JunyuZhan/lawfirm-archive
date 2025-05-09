<template>
  <div class="exception-page">
    <div class="exception-content">
      <div class="exception-image">
        <el-image :src="require('@/assets/images/404.svg')" alt="404" />
      </div>
      <h1 class="exception-title">404</h1>
      <div class="exception-desc">抱歉，您访问的页面不存在</div>
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