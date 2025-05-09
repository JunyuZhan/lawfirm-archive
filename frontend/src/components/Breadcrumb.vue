<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index" :to="item.path">
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const breadcrumbs = ref([])

// 路由映射表
const routeMap = {
  '': '首页',
  'cases': '案件管理',
  'categories': '分类管理',
  'system': '系统管理',
  'users': '用户管理',
  'roles': '角色管理',
  'profile': '个人中心'
}

// 处理面包屑
const getBreadcrumbs = () => {
  const matched = route.path.split('/')
  const result = []
  let path = ''
  
  matched.forEach((segment, index) => {
    if (!segment) return
    path += `/${segment}`
    
    const title = routeMap[segment] || segment
    result.push({
      path: path,
      title: title
    })
  })
  
  // 确保首页总是在第一位
  if (result.length > 0 && result[0].path !== '/') {
    result.unshift({
      path: '/',
      title: '首页'
    })
  }
  
  breadcrumbs.value = result
}

// 监听路由变化，更新面包屑
watch(() => route.path, () => {
  getBreadcrumbs()
}, { immediate: true })
</script>

<style scoped>
.el-breadcrumb {
  padding: 10px 0;
}
</style> 