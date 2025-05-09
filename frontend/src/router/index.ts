import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/components/layout/MainLayout.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'MainLayout',
    component: MainLayout,
    redirect: '/cases',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'cases',
        name: 'CaseManagement',
        component: () => import('@/views/CaseManagement.vue'),
        meta: { requiresAuth: true, title: '案件管理' }
      },
      {
        path: 'cases/:id',
        name: 'CaseDetail',
        component: () => import('@/views/CaseDetail.vue'),
        meta: { requiresAuth: true, title: '案件详情' }
      },
      {
        path: 'categories',
        name: 'CategoryManagement',
        component: () => import('@/views/CategoryManagement.vue'),
        meta: { requiresAuth: true, title: '分类管理' }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/system/UserProfile.vue'),
        meta: { requiresAuth: true, title: '个人中心' }
      },
      {
        path: 'system/users',
        name: 'UserManagement',
        component: () => import('@/views/system/UserManagement.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'], title: '用户管理' }
      },
      {
        path: 'system/roles',
        name: 'RoleManagement',
        component: () => import('@/views/system/RoleManagement.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'], title: '角色管理' }
      }
    ]
  },
  {
    path: '/403',
    name: '403',
    component: () => import('@/views/exception/403.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/exception/404.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')
  const user = userStr ? JSON.parse(userStr) : null
  
  // 判断是否需要认证
  if (to.meta.requiresAuth) {
    if (!token) {
      ElMessage.warning('请先登录')
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else {
      // 判断是否有访问角色权限
      if (to.meta.roles && Array.isArray(to.meta.roles) && to.meta.roles.length > 0) {
        const hasRole = to.meta.roles.some((role: string) => {
          return user && user.roles && Array.isArray(user.roles) && user.roles.some((userRole: any) => userRole.name === role)
        })
        
        if (hasRole) {
          next()
        } else {
          ElMessage.error('您没有权限访问此页面')
          next({ name: '403' })
        }
      } else {
        next()
      }
    }
  } else {
    // 如果已登录且要访问登录页，则重定向到首页
    if (token && to.path === '/login') {
      next({ path: '/' })
    } else {
      next()
    }
  }
})

export default router
