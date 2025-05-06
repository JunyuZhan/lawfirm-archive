import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/categories',
    name: 'CategoryManagement',
    component: () => import('@/views/CategoryManagement.vue')
  },
  {
    path: '/cases',
    name: 'CaseManagement',
    component: () => import('@/views/CaseManagement.vue')
  },
  {
    path: '/cases/:id',
    name: 'CaseDetail',
    component: () => import('@/views/CaseDetail.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  // 你可以在这里继续添加其他页面路由
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录只能访问首页和登录页，其他页面跳转到 /login，已登录访问 /login 跳转到首页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && to.path !== '/' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
