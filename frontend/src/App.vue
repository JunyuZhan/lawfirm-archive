<template>
  <div>
    <template v-if="isLogin">
      <el-container style="min-height: 100vh;">
        <el-header>
          <el-menu :default-active="route.path" mode="horizontal" router>
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/categories">分类管理</el-menu-item>
            <!-- 未来可继续添加：标签管理、案件管理等 -->
          </el-menu>
        </el-header>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </template>
    <template v-else>
      <router-view />
    </template>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'
const route = useRoute()
const isLogin = computed(() => !!localStorage.getItem('token'))
</script>

<style>
/* 全局样式重置，确保无边距和内边距，支持全屏布局 */
body, html {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow-x: hidden;
}
</style>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
