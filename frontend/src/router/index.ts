import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'

NProgress.configure({ showSpinner: false })

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/books',  // 默认重定向到图书列表
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Odometer', roles: ['ADMIN'] }  // 只有管理员可访问
      },
      {
        path: 'books',
        name: 'Books',
        component: () => import('@/views/book/BookList.vue'),
        meta: { title: '图书列表', icon: 'Reading' }
      },
      {
        path: 'books/add',
        name: 'BookAdd',
        component: () => import('@/views/book/BookForm.vue'),
        meta: { title: '新增图书', hidden: true }
      },
      {
        path: 'books/edit/:id',
        name: 'BookEdit',
        component: () => import('@/views/book/BookForm.vue'),
        meta: { title: '编辑图书', hidden: true }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/category/index.vue'),
        meta: { title: '分类管理', icon: 'Collection' }
      },
      {
        path: 'stock-warning',
        name: 'StockWarning',
        component: () => import('@/views/book/StockWarning.vue'),
        meta: { title: '库存预警', icon: 'Warning' }
      },
      {
        path: 'borrows',
        name: 'Borrows',
        component: () => import('@/views/borrow/BorrowList.vue'),
        meta: { title: '借阅记录', icon: 'Tickets' }
      },
      {
        path: 'my-borrows',
        name: 'MyBorrows',
        component: () => import('@/views/borrow/MyBorrows.vue'),
        meta: { title: '我的借阅', icon: 'Document' }
      },
      {
        path: 'overdue',
        name: 'Overdue',
        component: () => import('@/views/borrow/OverdueList.vue'),
        meta: { title: '逾期列表', icon: 'Clock' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'User', roles: ['ADMIN'] }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/role/RoleList.vue'),
        meta: { title: '角色管理', icon: 'Avatar', roles: ['ADMIN'] }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/system/LogList.vue'),
        meta: { title: '操作日志', icon: 'Document', roles: ['ADMIN'] }
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('@/views/system/ConfigList.vue'),
        meta: { title: '系统配置', icon: 'Setting', roles: ['ADMIN'] }
      },
      {
        path: 'backups',
        name: 'Backups',
        component: () => import('@/views/system/BackupList.vue'),
        meta: { title: '数据备份', icon: 'Files', roles: ['ADMIN'] }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/notification/NotificationList.vue'),
        meta: { title: '消息通知', icon: 'Bell' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心', hidden: true }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  
  document.title = `${to.meta.title || ''} - 图书管理系统`
  
  const userStore = useUserStore()
  const token = userStore.token
  
  // 不需要认证的页面
  if (to.meta.requiresAuth === false) {
    // 已登录用户访问登录或注册页面时，重定向到首页
    if (token && (to.path === '/login' || to.path === '/register')) {
      next('/')
    } else {
      next()
    }
    return
  }
  
  // 需要认证
  if (!token) {
    next(`/login?redirect=${to.path}`)
    return
  }
  
  // 检查角色权限
  if (to.meta.roles && Array.isArray(to.meta.roles)) {
    const hasRole = to.meta.roles.some(role => userStore.roles.includes(role as string))
    if (!hasRole) {
      next('/403')
      return
    }
  }
  
  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
