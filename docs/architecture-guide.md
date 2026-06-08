# 图书管理系统 — 技术架构理解文档

> 本文档基于对项目源码的完整通读，梳理前后端分层设计、核心数据模型、认证权限、借阅业务流转、关键接口与异常处理策略，供后续维护参考。

---

## 一、项目概览

本系统是一个前后端分离的图书管理系统，后端提供 RESTful API，前端为 SPA 单页应用，通过 Nginx 反向代理实现前后端联调。

| 维度 | 选型 |
|------|------|
| 后端框架 | Spring Boot 3.2.0 + Java 17 |
| 持久层 | MyBatis 3.0.3 + MySQL 8.0 |
| 安全框架 | Spring Security + JWT (jjwt 0.12.3) |
| 前端框架 | Vue 3.4 + TypeScript 5.6 + Vite 5 |
| UI 组件库 | Element Plus 2.5 |
| 状态管理 | Pinia 2.1 |
| HTTP 客户端 | Axios 1.6 |
| 部署方式 | Docker Compose（MySQL + Backend + Frontend/Nginx） |

后端服务端口 `9999`，上下文路径 `/api`；前端开发端口 `8081`，通过 Vite proxy 将 `/api` 请求代理至后端。

---

## 二、前后端分层设计与模块划分

### 2.1 后端分层

后端代码位于 `backend/src/main/java/com/library/`，采用 **按业务模块纵向切分 + 每模块内横向分层** 的组织方式。

```
com.library
├── LibraryApplication.java          # 启动类（@MapperScan + @EnableScheduling + @EnableAsync）
├── aspect/                          # AOP 切面
│   ├── OperationLog.java            #   操作日志注解
│   └── OperationLogAspect.java      #   操作日志切面实现
├── common/                          # 公共基础层
│   ├── base/                        #   BaseMapper / BaseService / BaseServiceImpl / BaseEntity
│   ├── config/                      #   SwaggerConfig / WebMvcConfig / MailConfig
│   ├── exception/                   #   BusinessException / GlobalExceptionHandler
│   ├── response/                    #   Result / ResultCode / PageResult
│   ├── service/                     #   MailService / MockMailServiceImpl
│   ├── util/                        #   SecurityUtil / IpUtil
│   └── xss/                         #   XssFilter / XssConfig / XssHttpServletRequestWrapper / XssUtil
├── module/                          # 业务模块层（每个模块内 controller/dto/entity/mapper/service/vo）
│   ├── auth/                        #   认证模块
│   ├── book/                        #   图书模块
│   ├── borrow/                      #   借阅模块
│   ├── category/                    #   分类模块
│   ├── notification/                #   通知模块
│   ├── role/                        #   角色权限模块
│   ├── system/                      #   系统管理模块
│   └── user/                        #   用户模块
├── security/                        # 安全层
│   ├── SecurityConfig.java
│   ├── LoginUser.java
│   ├── JwtAccessDeniedHandler.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── filter/JwtAuthenticationFilter.java
│   └── jwt/JwtTokenProvider.java
└── task/                            # 定时任务
    └── BorrowReminderTask.java
```

**模块内横向分层规范**（以 `book` 模块为例）：

| 层 | 目录 | 职责 |
|----|------|------|
| Controller | `controller/BookController.java` | 接收请求、参数校验、权限控制、调用 Service、返回 `Result<T>` |
| DTO | `dto/BookDTO.java` / `BookQueryDTO.java` / `StockDTO.java` | 入参传输对象 |
| Entity | `entity/Book.java` | 数据库实体映射 |
| Mapper | `mapper/BookMapper.java` | MyBatis Mapper 接口 |
| Service | `service/BookService.java` | 业务接口，继承 `BaseService<Book>` |
| VO | `vo/BookImportResultVO.java` | 出参视图对象 |

**通用基类体系**：

- `BaseEntity` — 包含 `id`、`createdAt`、`updatedAt`，`Book` 和 `User` 继承此类
- `BaseMapper<T>` — 定义 `selectById`、`selectAll`、`selectByCondition`、`selectPage`、`countByCondition`、`insert`、`batchInsert`、`updateById`、`deleteById`、`deleteBatchIds`、`exists` 共 11 个通用方法
- `BaseService<T>` — 定义 `getById`、`listAll`、`listByCondition`、`page`、`save`、`saveBatch`、`updateById`、`removeById`、`removeByIds`、`count` 共 10 个通用方法
- `BaseServiceImpl<M, T>` — 实现 BaseService，通过 `@Autowired M baseMapper` 委托给 BaseMapper

### 2.2 前端分层

前端代码位于 `frontend/src/`，按功能职责横向分层：

```
src/
├── main.ts              # 应用入口（Pinia + Router + ElementPlus 中文）
├── App.vue              # 根组件
├── api/                 # API 接口层（按模块拆分）
│   ├── auth.ts          #   认证接口
│   ├── book.ts          #   图书接口
│   ├── borrow.ts        #   借阅接口
│   ├── category.ts      #   分类接口
│   ├── notification.ts  #   通知接口
│   ├── role.ts          #   角色接口
│   ├── user.ts          #   用户接口
│   ├── system.ts        #   系统管理接口
│   └── backup.ts        #   备份接口
├── layouts/             # 布局组件
│   └── MainLayout.vue   #   主布局（侧边栏 + 顶栏 + 内容区）
├── router/              # 路由配置
│   └── index.ts         #   路由表 + 路由守卫
├── stores/              # Pinia 状态管理
│   └── user.ts          #   用户状态（Token、角色权限、localStorage 持久化）
├── styles/              # 全局样式
│   └── index.scss
├── utils/               # 工具函数
│   ├── request.ts       #   Axios 封装（拦截器、Token 注入、错误处理）
│   ├── download.ts      #   文件下载工具
│   ├── format.ts        #   格式化工具
│   └── validators.ts    #   表单校验规则
└── views/               # 页面视图
    ├── book/            #   BookList / BookForm / StockWarning
    ├── borrow/          #   BorrowList / MyBorrows / OverdueList
    ├── category/        #   分类管理
    ├── dashboard/       #   仪表盘
    ├── error/           #   403 / 404
    ├── login/           #   登录 / 注册
    ├── notification/    #   通知列表
    ├── role/            #   角色管理
    ├── system/          #   日志 / 配置 / 备份
    └── user/            #   用户列表 / 个人中心
```

**前端请求链路**：`View 组件` → `api/xxx.ts` → `utils/request.ts`（Axios 实例）→ 后端 `/api/xxx`

---

## 三、核心数据模型与表关系

### 3.1 数据库表清单

共 10 张表，定义在 `backend/src/main/resources/db/schema.sql`：

| 表名 | 说明 | 主键 | 核心索引 |
|------|------|------|----------|
| `users` | 用户表 | `id BIGINT AUTO_INCREMENT` | `idx_username`, `idx_email`, `idx_status` |
| `roles` | 角色表 | `id BIGINT AUTO_INCREMENT` | `idx_role_code` |
| `user_roles` | 用户-角色关联表 | `id BIGINT AUTO_INCREMENT` | `idx_user_role(user_id, role_id)` |
| `permissions` | 权限表 | `id BIGINT AUTO_INCREMENT` | `idx_permission_code`, `idx_parent_id` |
| `role_permissions` | 角色-权限关联表 | `id BIGINT AUTO_INCREMENT` | `idx_role_permission(role_id, permission_id)` |
| `categories` | 图书分类表 | `id BIGINT AUTO_INCREMENT` | `idx_parent_id`, `idx_name` |
| `books` | 图书表 | `id BIGINT AUTO_INCREMENT` | `idx_isbn`, `idx_title`, `idx_category_id`, `idx_status` |
| `borrow_records` | 借阅记录表 | `id BIGINT AUTO_INCREMENT` | `idx_user_id`, `idx_book_id`, `idx_status`, `idx_due_date` |
| `notifications` | 通知表 | `id BIGINT AUTO_INCREMENT` | `idx_user_id`, `idx_is_read`, `idx_type` |
| `operation_logs` | 操作日志表 | `id BIGINT AUTO_INCREMENT` | `idx_user_id`, `idx_created_at` |
| `system_config` | 系统配置表 | `id BIGINT AUTO_INCREMENT` | `idx_config_key` |

### 3.2 实体继承关系

```
BaseEntity (id, createdAt, updatedAt)
├── Book (isbn, title, author, publisher, publishDate, price, categoryId, coverUrl, description, totalStock, availableStock, location, status, transient categoryName)
└── User (username, password, email, phone, avatar, status)

Serializable（独立定义 id 和时间字段）
├── BorrowRecord (userId, bookId, borrowDate, dueDate, returnDate, renewCount, fineAmount, status, remark, transient username/bookTitle/isbn/author)
├── Role (roleName, roleCode, description)
├── Permission (permissionName, permissionCode, resourceType, parentId, sortOrder, transient children)
├── Category (name, description, parentId, sortOrder, transient children)
├── Notification (userId, title, content, type, isRead)
├── SystemConfig (configKey, configValue, description)
└── OperationLogEntity (userId, username, operation, method, params, ip, timeCost, status, errorMsg)
```

### 3.3 表关系 ER 图（文字描述）

```
users ──1:N── user_roles ──N:1── roles
roles ──1:N── role_permissions ──N:1── permissions (树形自关联: parent_id)

users ──1:N── borrow_records ──N:1── books
books  ──N:1── categories (树形自关联: parent_id)

users ──1:N── notifications
users ──1:N── operation_logs
```

- **用户-角色**：多对多，通过 `user_roles` 关联
- **角色-权限**：多对多，通过 `role_permissions` 关联
- **权限**：支持树形结构（`parent_id` 自关联），`resource_type` 区分菜单/按钮
- **分类**：支持树形结构（`parent_id` 自关联）
- **借阅记录**：关联 `users` 和 `books`，Mapper XML 中通过 LEFT JOIN 查询用户名和图书信息

### 3.4 初始数据

定义在 `backend/src/main/resources/db/data.sql`：

- **角色**：ADMIN（管理员）、USER（普通用户）
- **用户**：admin/user/test，密码均为 `123456`（BCrypt 加密）
- **权限树**：5 个一级菜单（仪表盘、图书管理、借阅管理、用户管理、系统管理），下设子菜单和按钮权限
- **管理员权限**：拥有全部 26 个权限
- **普通用户权限**：仪表盘、图书管理（查看）、我的借阅、借阅/归还/续借操作，共 7 个权限

---

## 四、用户认证与权限控制

### 4.1 认证流程

```
前端                          后端
 │                              │
 │  POST /api/auth/login        │
 │  {username, password,        │
 │   captchaKey, captchaCode,   │
 │   smsCode}                   │
 │ ──────────────────────────>  │
 │                              │ AuthService.login()
 │                              │   1. 校验图形验证码
 │                              │   2. 校验短信验证码
 │                              │   3. 验证用户名密码（BCrypt）
 │                              │   4. 加载角色权限
 │                              │   5. 生成 accessToken + refreshToken
 │                              │
 │  <────────────────────────── │
 │  {accessToken, refreshToken, │
 │   userId, username, roles,   │
 │   permissions}               │
 │                              │
 │  后续请求: Authorization:    │
 │  Bearer <accessToken>        │
 │ ──────────────────────────>  │
 │                              │ JwtAuthenticationFilter
 │                              │   1. 提取 Bearer Token
 │                              │   2. JwtTokenProvider.validateToken()
 │                              │   3. 解析 userId/username/roles/permissions
 │                              │   4. 构建 LoginUser → SecurityContext
```

**关键组件**：

| 组件 | 文件 | 职责 |
|------|------|------|
| `SecurityConfig` | `security/SecurityConfig.java` | 关闭 CSRF、无状态 Session、白名单路径、注册 JWT 过滤器、配置 401/403 处理器 |
| `JwtTokenProvider` | `security/jwt/JwtTokenProvider.java` | Token 生成/解析/验证，accessToken 24h、refreshToken 7d，Claims 包含 userId/username/roles/permissions |
| `JwtAuthenticationFilter` | `security/filter/JwtAuthenticationFilter.java` | 继承 `OncePerRequestFilter`，从 Header 提取 Token，验证后设置 SecurityContext |
| `LoginUser` | `security/LoginUser.java` | 实现 `UserDetails`，封装 userId/username/roles/permissions，`getAuthorities()` 合并角色（ROLE_前缀）和权限 |
| `SecurityUtil` | `common/util/SecurityUtil.java` | 工具类，从 SecurityContext 获取当前用户信息 |

**白名单路径**（无需 Token）：

```
/auth/login, /auth/register, /auth/send-sms-code,
/auth/refresh-token, /auth/captcha, /auth/logout,
/swagger-ui/**, /v3/api-docs/**, /uploads/**, /error
```

### 4.2 权限模型（RBAC）

采用 **基于角色的访问控制（RBAC）**，双层校验：

**第一层：后端方法级权限**

通过 `@PreAuthorize("hasRole('ADMIN')")` 注解控制，如：

- `BorrowController.page()` — `@PreAuthorize("hasRole('ADMIN')")`
- `RoleController` 类级 — `@PreAuthorize("hasRole('ADMIN')")`
- `BookController.create()` — `@PreAuthorize("hasRole('ADMIN')")`

`LoginUser.getAuthorities()` 将角色转为 `ROLE_ADMIN` / `ROLE_USER` 格式的 `GrantedAuthority`，Spring Security 的 `hasRole('ADMIN')` 会自动匹配 `ROLE_ADMIN`。

**第二层：前端路由级权限**

`router/index.ts` 路由守卫中检查 `meta.roles`：

```typescript
if (to.meta.roles && Array.isArray(to.meta.roles)) {
  const hasRole = to.meta.roles.some(role => userStore.roles.includes(role))
  if (!hasRole) {
    next('/403')
  }
}
```

管理员专属路由：`/dashboard`、`/users`、`/roles`、`/logs`、`/config`、`/backups`、`/borrows`（借阅记录）、`/overdue`、`/stock-warning`。

**前端菜单可见性**：`MainLayout.vue` 中通过 `v-if="isAdmin"` 控制管理员菜单项的显示。

### 4.3 Token 刷新机制

- `accessToken` 有效期 24 小时，`refreshToken` 有效期 7 天
- 前端 `request.ts` 中预留了 Token 刷新队列机制（`isRefreshing` + `retryQueue`）
- 刷新接口：`POST /api/auth/refresh-token`，传入 `refreshToken` 返回新的 `LoginVO`

### 4.4 前端 Token 管理

`stores/user.ts`（Pinia Store）：

- 登录成功后将 `accessToken`、`refreshToken`、`username`、`roles`、`permissions` 存入 `localStorage`
- 页面刷新时从 `localStorage` 恢复状态
- 退出登录时清除 `localStorage` 并跳转登录页

---

## 五、借阅业务状态流转

### 5.1 借阅记录状态定义

`BorrowRecord.status` 字段：

| 值 | 含义 | 说明 |
|----|------|------|
| 0 | 借阅中 | 图书尚未归还，可能在借期内也可能已逾期 |
| 1 | 已归还 | 图书已归还，`returnDate` 已填写 |
| 2 | 逾期 | 应还日期已过且未归还 |

### 5.2 状态流转图

```
                    ┌──────────────────────────────┐
                    │                              │
                    ▼                              │
  [借阅] ──> status=0(借阅中) ──> [续借] ──> status=0(借阅中, dueDate顺延)
                    │                              │
                    │ dueDate < now                 │ 最多续借1次
                    │ (定时任务更新)                 │
                    ▼                              │
              status=2(逾期) ──────────────────────┘
                    │
                    │ [归还]
                    ▼
              status=1(已归还)
              returnDate=now
              fineAmount=逾期天数×0.5元/天
```

### 5.3 核心业务规则

| 规则 | 配置值 | 来源 |
|------|--------|------|
| 默认借阅天数 | 30 天 | `application.yml: library.default-borrow-days` / `system_config` 表 |
| 最大续借次数 | 1 次 | `application.yml: library.max-renew-times` / `system_config` 表 |
| 续借天数 | 30 天 | `application.yml: library.renew-days` / `system_config` 表 |
| 逾期罚款 | 0.5 元/天 | `application.yml: library.overdue-fine-per-day` / `system_config` 表 |
| 最大借阅数量 | 5 本 | `system_config: max_borrow_count` |
| 库存预警阈值 | 5 本 | `system_config: stock_warning_threshold` |

### 5.4 借书流程

入口：`BorrowController.borrow()` → `BorrowService.borrowBook(userId, bookId)`

1. 校验图书是否存在且上架（`status=1`）
2. 校验可用库存（`availableStock > 0`）
3. 校验用户当前借阅数量是否达上限（`countBorrowingByUserId < max_borrow_count`）
4. 校验用户是否已借阅同一本书（`selectBorrowingByUserIdAndBookId` 查询 status IN (0,2) 的记录）
5. 扣减图书可用库存（`decreaseStock`）
6. 创建借阅记录（`status=0`，`borrowDate=now`，`dueDate=now+30天`，`renewCount=0`）

管理员代借阅：`BorrowController.adminBorrow()` → 同样调用 `BorrowService.borrowBook(userId, bookId)`，区别在于 userId 由管理员指定。

### 5.5 还书流程

入口：`BorrowController.returnBook()` → `BorrowService.returnBook(borrowId)`

1. 查询借阅记录，校验状态为借阅中或逾期（`status IN (0, 2)`）
2. 计算罚款：若 `dueDate < now`，`fineAmount = 逾期天数 × 0.5`
3. 更新借阅记录：`status=1`，`returnDate=now`，`fineAmount`
4. 恢复图书可用库存（`increaseStock`）

### 5.6 续借流程

入口：`BorrowController.renewBook()` → `BorrowService.renewBook(borrowId)`

1. 查询借阅记录，校验状态为借阅中（`status=0`）
2. 校验续借次数是否达上限（`renewCount >= max_renew_times`）
3. 更新借阅记录：`dueDate = dueDate + 30天`，`renewCount += 1`

### 5.7 逾期处理

**定时任务自动更新逾期状态**：

`BorrowReminderTask` 中通过 `BorrowRecordMapper.updateOverdueStatus` 执行：

```sql
UPDATE borrow_records SET status = 2 WHERE status = 0 AND due_date < #{now}
```

此操作将所有应还日期已过且状态仍为"借阅中"的记录更新为"逾期"。

**到期提醒邮件**（每天 8:00）：

- 查询 `status=0` 且 `dueDate` 在未来 N 天内（默认 3 天）的记录
- 调用 `MailService.sendBorrowReminderMail()` 发送提醒邮件

**逾期通知邮件**（每天 9:00）：

- 查询 `status IN (0,2)` 且 `dueDate < today` 的记录
- 计算逾期天数和罚款金额
- 调用 `MailService.sendOverdueNoticeMail()` 发送通知邮件

---

## 六、前后端交互关键接口与异常处理策略

### 6.1 API 接口总览

所有接口前缀为 `/api`，后端上下文路径配置在 `application.yml: server.servlet.context-path`。

#### 认证模块 `/api/auth`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/auth/login` | 公开 | 用户登录，返回 JWT Token |
| POST | `/auth/register` | 公开 | 用户注册 |
| POST | `/auth/send-sms-code` | 公开 | 发送短信验证码 |
| GET | `/auth/captcha` | 公开 | 获取图形验证码 |
| POST | `/auth/refresh-token` | 公开 | 刷新 Token |
| POST | `/auth/logout` | 公开 | 退出登录（客户端删除 Token） |

#### 图书模块 `/api/books`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/books` | 登录 | 分页查询图书 |
| GET | `/books/{id}` | 登录 | 获取图书详情 |
| POST | `/books` | ADMIN | 新增图书 |
| PUT | `/books/{id}` | ADMIN | 更新图书 |
| DELETE | `/books/{id}` | ADMIN | 删除图书 |
| POST | `/books/{id}/stock` | ADMIN | 调整库存 |
| GET | `/books/stock-warning` | ADMIN | 库存预警列表 |
| POST | `/books/cover/upload` | ADMIN | 上传封面 |
| POST | `/books/import` | ADMIN | JSON 批量导入 |
| POST | `/books/import/excel` | ADMIN | Excel 批量导入 |
| GET | `/books/import/template` | ADMIN | 下载导入模板 |
| GET | `/books/export` | ADMIN | 导出图书（JSON） |
| GET | `/books/export/excel` | ADMIN | 导出图书（Excel） |
| PUT | `/books/{id}/status` | ADMIN | 更新图书状态 |

#### 借阅模块 `/api/borrows`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/borrows` | ADMIN | 分页查询借阅记录 |
| GET | `/borrows/{id}` | 登录 | 获取借阅详情 |
| POST | `/borrows` | 登录 | 借阅图书 |
| POST | `/borrows/admin` | ADMIN | 管理员代借阅 |
| PUT | `/borrows/{id}/return` | 登录 | 归还图书 |
| PUT | `/borrows/{id}/renew` | 登录 | 续借图书 |
| GET | `/borrows/my` | 登录 | 我的借阅记录 |
| GET | `/borrows/current` | 登录 | 当前借阅列表 |
| GET | `/borrows/overdue` | ADMIN | 逾期列表 |
| GET | `/borrows/statistics` | ADMIN | 借阅统计 |
| GET | `/borrows/can-borrow` | 登录 | 检查是否可借阅 |

#### 用户模块 `/api/users`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/users` | ADMIN | 分页查询用户 |
| GET | `/users/{id}` | ADMIN | 获取用户详情 |
| POST | `/users` | ADMIN | 新增用户 |
| PUT | `/users/{id}` | ADMIN | 更新用户 |
| DELETE | `/users/{id}` | ADMIN | 删除用户 |
| PUT | `/users/{id}/status` | ADMIN | 修改用户状态 |
| PUT | `/users/{id}/reset-password` | ADMIN | 重置密码 |
| PUT | `/users/{id}/roles` | ADMIN | 分配角色 |
| GET | `/users/profile` | 登录 | 获取当前用户信息 |
| PUT | `/users/profile` | 登录 | 更新个人信息 |
| PUT | `/users/password` | 登录 | 修改密码 |

#### 角色模块 `/api/roles`（全部 ADMIN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/roles` | 分页查询角色 |
| GET | `/roles/all` | 获取所有角色 |
| GET | `/roles/{id}` | 角色详情 |
| POST | `/roles` | 新增角色 |
| PUT | `/roles/{id}` | 更新角色 |
| DELETE | `/roles/{id}` | 删除角色 |
| GET | `/roles/{id}/permissions` | 获取角色权限 |
| PUT | `/roles/{id}/permissions` | 分配角色权限 |
| GET | `/roles/permissions` | 获取权限树 |

#### 分类模块 `/api/categories`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/categories` | 登录 | 分类列表 |
| GET | `/categories/tree` | 登录 | 分类树 |
| GET | `/categories/{id}` | 登录 | 分类详情 |
| POST | `/categories` | ADMIN | 新增分类 |
| PUT | `/categories/{id}` | ADMIN | 更新分类 |
| DELETE | `/categories/{id}` | ADMIN | 删除分类 |

#### 通知模块 `/api/notifications`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/notifications` | 登录 | 通知列表 |
| GET | `/notifications/unread-count` | 登录 | 未读数量 |
| PUT | `/notifications/{id}/read` | 登录 | 标记已读 |
| PUT | `/notifications/read-all` | 登录 | 全部标记已读 |

#### 系统模块 `/api/system`（全部 ADMIN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/logs` | 操作日志 |
| GET | `/system/config` | 系统配置 |
| PUT | `/system/config` | 更新配置 |
| GET | `/system/dashboard` | 仪表盘数据 |
| POST | `/system/backup` | 创建备份 |
| GET | `/system/backups` | 备份列表 |
| POST | `/system/backup/restore` | 恢复备份 |
| GET | `/system/backup/download/{filename}` | 下载备份 |
| DELETE | `/system/backup/{filename}` | 删除备份 |
| DELETE | `/system/backups/clean` | 清理过期备份 |
| DELETE | `/system/logs/clean` | 清理历史日志 |

### 6.2 统一响应格式

后端所有接口返回 `Result<T>`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": T,
  "timestamp": 1700000000000
}
```

分页响应 `data` 为 `PageResult<T>`：

```json
{
  "list": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 10,
  "pages": 0,
  "hasPrevious": false,
  "hasNext": false
}
```

### 6.3 异常处理策略

#### 后端异常处理

`GlobalExceptionHandler`（`@RestControllerAdvice`）统一捕获异常并转为 `Result<Void>`：

| 异常类型 | HTTP 状态码 | ResultCode | 说明 |
|----------|-------------|------------|------|
| `BusinessException` | 200 | 自定义 code | 业务异常，由 Service 层主动抛出 |
| `MethodArgumentNotValidException` | 400 | 1001 | `@RequestBody` 参数校验失败 |
| `ConstraintViolationException` | 400 | 1001 | `@RequestParam`/`@PathVariable` 校验失败 |
| `BindException` | 400 | 1001 | 参数绑定失败 |
| `MissingServletRequestParameterException` | 400 | 1001 | 缺少必要参数 |
| `MethodArgumentTypeMismatchException` | 400 | 1001 | 参数类型错误 |
| `HttpMessageNotReadableException` | 400 | 1001 | 请求体格式错误 |
| `MaxUploadSizeExceededException` | 400 | 1001 | 文件上传超限 |
| `AuthenticationException` | 401 | 401/2002 | 认证失败（密码错误返回 2002） |
| `AccessDeniedException` | 403 | 403 | 权限不足 |
| `NoHandlerFoundException` | 404 | 404 | 接口不存在 |
| `HttpRequestMethodNotSupportedException` | 405 | 405 | 请求方法不允许 |
| `Exception` | 500 | 500 | 兜底：未知异常，返回"系统繁忙" |

**业务异常码体系**（`ResultCode` 枚举）：

| 范围 | 分类 | 示例 |
|------|------|------|
| 200 | 成功 | `SUCCESS(200)` |
| 400-409 | HTTP 客户端错误 | `UNAUTHORIZED(401)`, `FORBIDDEN(403)` |
| 500-503 | HTTP 服务端错误 | `ERROR(500)` |
| 1xxx | 通用业务错误 | `PARAM_ERROR(1001)`, `DATA_NOT_EXIST(1002)`, `DATA_ALREADY_EXIST(1003)` |
| 2xxx | 用户相关 | `USER_NOT_EXIST(2001)`, `USER_PASSWORD_ERROR(2002)`, `CAPTCHA_ERROR(2005)`, `SMS_CODE_ERROR(2008)` |
| 3xxx | 图书相关 | `BOOK_NOT_EXIST(3001)`, `BOOK_STOCK_NOT_ENOUGH(3002)`, `BOOK_ISBN_EXIST(3003)` |
| 4xxx | 借阅相关 | `BORROW_LIMIT_EXCEEDED(4001)`, `BOOK_ALREADY_BORROWED(4003)`, `RENEW_LIMIT_EXCEEDED(4004)`, `BORROW_OVERDUE(4006)` |

#### 前端异常处理

`utils/request.ts` Axios 响应拦截器：

1. **业务错误**（`res.code !== 200`）：弹出 `ElMessage.error` 提示
2. **401 未认证**：弹出 `ElMessageBox.confirm` 提示"登录已过期"，确认后跳转登录页（防重复弹窗）
3. **403 无权限**：`ElMessage.error('没有操作权限')`
4. **404**：`ElMessage.error('请求的资源不存在')`
5. **500**：`ElMessage.error('服务器内部错误')`
6. **请求超时**：`ElMessage.error('请求超时')`
7. **网络断开**：`ElMessage.error('网络连接失败')`

### 6.4 操作日志

通过 AOP 切面 `OperationLogAspect` 实现，标注 `@OperationLog` 注解的接口自动记录：

- 操作描述、操作类型（CREATE/UPDATE/DELETE/QUERY/IMPORT/EXPORT/LOGIN/LOGOUT/OTHER）
- 请求方法、请求参数（过滤 MultipartFile，截断至 2000 字符）
- 操作用户（从 SecurityContext 获取，异常时记为 anonymous）
- IP 地址、耗时（ms）、成功/失败状态及错误信息

---

## 七、定时任务

| 任务 | Cron 表达式 | 配置项 | 说明 |
|------|-------------|--------|------|
| 到期提醒 | `0 0 8 * * ?` | `library.reminder.cron` | 每天 8:00，提前 N 天发送到期提醒邮件 |
| 逾期通知 | `0 0 9 * * ?` | `library.overdue.cron` | 每天 9:00，发送逾期通知邮件 |
| 数据备份 | `0 0 2 * * ?` | `library.backup.cron` | 每天凌晨 2:00，创建数据库备份 |

定时任务定义在 `task/BorrowReminderTask.java`，通过 `@Scheduled` 注解驱动，可通过配置项 `library.reminder.enabled` 开关控制。

---

## 八、部署架构

Docker Compose 编排三个服务：

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  frontend    │     │   backend    │     │    mysql     │
│  (Nginx)     │────>│  (Spring)    │────>│   (MySQL)    │
│  Port: 8081  │     │  Port: 9999  │     │  Port: 3307  │
└──────────────┘     └──────────────┘     └──────────────┘
     :80 → Nginx          :9999               :3306
     /api → proxy         /api                library_db
     到 backend:9999
```

- MySQL 初始化时自动执行 `schema.sql` 和 `data.sql`
- 后端通过环境变量 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` 连接数据库
- 前端 Nginx 配置将 `/api` 请求反向代理到后端
- 文件上传目录 `/app/uploads` 和备份目录 `/app/backups` 使用 Docker Volume 持久化

---

## 九、安全防护措施

| 措施 | 实现位置 | 说明 |
|------|----------|------|
| XSS 过滤 | `common/xss/XssFilter.java` | 全局 XSS 过滤器，包装 HttpServletRequest |
| CSRF 防护 | `SecurityConfig` | 无状态 JWT 方案，关闭 CSRF |
| 安全响应头 | `SecurityConfig` | XSS-Protection、Content-Type-Options、X-Frame-Options、CSP |
| 密码加密 | `BCryptPasswordEncoder` | Spring Security 默认 BCrypt |
| JWT 签名 | `JwtTokenProvider` | HMAC-SHA 算法，密钥 Base64 编码 |
| 文件上传限制 | `application.yml` | 最大 5MB，限制文件类型 |
| 参数校验 | `@Valid` + `@NotBlank` 等 | Jakarta Validation 注解 |
| 敏感信息脱敏 | `UserController` | 返回用户信息时 `password` 置 null |

---

## 十、关键代码引用索引

| 关注点 | 后端文件 | 前端文件 |
|--------|----------|----------|
| 应用入口 | `LibraryApplication.java` | `main.ts` |
| 安全配置 | `security/SecurityConfig.java` | `router/index.ts` |
| JWT 处理 | `security/jwt/JwtTokenProvider.java` | `utils/request.ts` |
| JWT 过滤器 | `security/filter/JwtAuthenticationFilter.java` | — |
| 登录用户 | `security/LoginUser.java` | `stores/user.ts` |
| 认证接口 | `module/auth/controller/AuthController.java` | `api/auth.ts` |
| 用户管理 | `module/user/controller/UserController.java` | `api/user.ts` |
| 角色权限 | `module/role/controller/RoleController.java` | `api/role.ts` |
| 图书管理 | `module/book/controller/BookController.java` | `api/book.ts` |
| 借阅管理 | `module/borrow/controller/BorrowController.java` | `api/borrow.ts` |
| 分类管理 | `module/category/controller/CategoryController.java` | `api/category.ts` |
| 通知管理 | `module/notification/controller/NotificationController.java` | `api/notification.ts` |
| 系统管理 | `module/system/controller/SystemController.java` | `api/system.ts` |
| 借阅定时任务 | `task/BorrowReminderTask.java` | — |
| 异常处理 | `common/exception/GlobalExceptionHandler.java` | `utils/request.ts` |
| 业务异常 | `common/exception/BusinessException.java` | — |
| 响应封装 | `common/response/Result.java` / `ResultCode.java` / `PageResult.java` | — |
| 操作日志 | `aspect/OperationLog.java` / `OperationLogAspect.java` | — |
| 数据库建表 | `resources/db/schema.sql` | — |
| 初始数据 | `resources/db/data.sql` | — |
| 主布局 | — | `layouts/MainLayout.vue` |
| 通用基类 | `common/base/BaseMapper.java` / `BaseService.java` / `BaseServiceImpl.java` / `BaseEntity.java` | — |
