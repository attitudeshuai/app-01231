# 图书管理系统 — 架构理解文档

> 本文档为通读项目代码后整理的架构理解材料，目的是帮助后续维护者快速建立对系统整体设计的认识。文档保持当前的前后端分离架构与既有技术栈，仅做"理解性"梳理，不提出架构层面的重构建议。

---

## 1. 项目总览

- **形态**：前后端分离 + Docker Compose 一键部署
- **技术栈**：
  - 后端：Spring Boot 3.2.0、Spring Security、MyBatis 3.0.3、MySQL 8.0、JWT (jjwt 0.12.3)、Hutool、EasyExcel、SpringDoc OpenAPI（见 [pom.xml](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/pom.xml)）
  - 前端：Vue 3.4 + TypeScript + Vite 5、Vue Router 4、Pinia 2、Element Plus、Axios、Day.js、NProgress（见 [package.json](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/package.json)）
  - 部署：Docker Compose 编排 MySQL / 后端 Spring Boot / 前端 Nginx 三容器（见 [docker-compose.yml](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/docker-compose.yml)）
- **服务端口**：前端 8081（Nginx）、后端 9999、MySQL 3307；后端 context-path 为 `/api`，前端 Nginx 通过 `location /api` 反向代理到 `backend:9999`（见 [nginx.conf](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/nginx.conf#L21-L31)）。
- **启动入口**：[LibraryApplication.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/LibraryApplication.java) 同时启用 `@MapperScan`、`@EnableScheduling`（定时任务）、`@EnableAsync`（异步执行）。

---

## 2. 后端分层与模块划分

### 2.1 包结构

后端代码统一放在 `com.library` 下，按"通用 + 安全 + 切面 + 任务 + 业务模块"分包：

| 顶层包 | 职责 |
| --- | --- |
| `com.library.common` | 跨模块基础设施：基类（`base`）、配置（`config`）、异常（`exception`）、统一响应（`response`）、邮件等服务（`service`）、工具（`util`）、XSS 防护（`xss`） |
| `com.library.security` | Spring Security 配置、JWT 工具、过滤器、登录用户对象、未认证 / 未授权处理器 |
| `com.library.aspect` | AOP 切面，目前实现操作日志切面（`OperationLogAspect`） |
| `com.library.task` | Spring 定时任务（如 `BorrowReminderTask`） |
| `com.library.module.*` | 业务模块，每个模块按 `controller / service / mapper / entity / dto / vo` 等子包组织 |

业务模块包括：`auth`（认证）、`book`（图书）、`borrow`（借阅）、`category`（分类）、`notification`（通知）、`role`（角色权限）、`system`（操作日志/系统配置/备份）、`user`（用户）。

### 2.2 通用分层模型

每个业务模块大致遵循同一套分层（以借阅为例）：

```
controller  →  service (interface)  →  service.impl  →  mapper (interface)  →  XxxMapper.xml  →  MySQL
                                                          ↑
                                          entity / dto / vo / query
```

- **Controller**：仅做参数接收、权限注解（`@PreAuthorize`）、调用 Service、包装统一响应 [Result](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/response/Result.java)。
- **Service / Service.Impl**：核心业务编排，事务边界（`@Transactional(rollbackFor = Exception.class)`）放在 Service 实现层。
- **Mapper + XML**：使用原生 MyBatis（非 MyBatis-Plus），分页通过 `offset / limit` 显式实现。
- **Entity / DTO / VO**：严格区分。`entity` 与数据库表对应；`DTO` 接收请求/查询参数；`VO` 用于返回前端的视图对象（如 [LoginVO.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/auth/vo/LoginVO.java)）。

### 2.3 通用基类

- [BaseEntity.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/base/BaseEntity.java)：所有实体的统一父类，包含 `id / createdAt / updatedAt`。
- [BaseMapper.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/base/BaseMapper.java)：声明通用 CRUD 方法（`selectById / selectAll / selectByCondition / selectPage / countByCondition / insert / batchInsert / updateById / deleteById / deleteBatchIds / exists`），具体 SQL 在每个模块的 XML 中实现。
- [BaseService.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/base/BaseService.java) / [BaseServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/base/BaseServiceImpl.java)：通用 Service 抽象与默认实现，注入 `baseMapper` 完成基础 CRUD 与分页（`page(...)`，参见 [BaseServiceImpl#page](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/base/BaseServiceImpl.java#L41-L53)）。
- 业务 Service 通常继承 `BaseServiceImpl<XxxMapper, XxxEntity>` 并实现自己的接口，从而自然获得通用 CRUD 能力。

### 2.4 统一响应与异常处理

- 所有 HTTP 接口统一返回 [Result](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/response/Result.java)，结构 `{ code, message, data, timestamp }`。
- 状态码集中维护在 [ResultCode.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/response/ResultCode.java)，按业务域分段：通用 4xx/5xx、`1xxx` 通用业务、`2xxx` 用户、`3xxx` 图书、`4xxx` 借阅。
- 业务侧主动抛出 [BusinessException.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/exception/BusinessException.java)（以 `ResultCode` 构造），由 [GlobalExceptionHandler.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/exception/GlobalExceptionHandler.java) 集中拦截。
- 全局异常处理覆盖：业务异常、JSR-303 校验（`MethodArgumentNotValidException`、`ConstraintViolationException`、`BindException`）、参数缺失/类型不匹配、请求体不可读、HTTP 方法不支持、404、文件大小超限、`AuthenticationException`（其中 `BadCredentialsException` 单独映射为 `USER_PASSWORD_ERROR`）、`AccessDeniedException`，最后兜底 `Exception` 返回"系统繁忙，请稍后重试"。

### 2.5 横切关注点

- **CORS / 静态资源**：[WebMvcConfig.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/config/WebMvcConfig.java) 放开所有 origin（`allowedOriginPatterns("*")`）和常用 HTTP 方法，并把 `/uploads/**` 映射到本地上传目录。
- **XSS 防护**：[XssFilter.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/xss/XssFilter.java) + `XssHttpServletRequestWrapper` + `XssUtil`，由 [XssConfig.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/xss/XssConfig.java) 注册到 Servlet 过滤器链。
- **操作日志**：通过 [@OperationLog](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/aspect/OperationLog.java) 注解 + [OperationLogAspect](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/aspect/OperationLogAspect.java) 环绕通知，自动记录"操作描述、调用方法、参数、IP、耗时、当前用户、成功/失败状态、错误信息"，写入 `operation_logs` 表。
- **定时任务**：
  - [BorrowServiceImpl#updateOverdueStatus](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L241-L245) 每天 01:00 扫描更新逾期状态。
  - [BorrowServiceImpl#sendDueReminder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L247-L269) 每天 09:00 写入到期站内通知。
  - [BorrowReminderTask](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/task/BorrowReminderTask.java) 配合 `library.reminder.cron / library.overdue.cron` 发送邮件提醒/逾期通知。
  - 备份任务由 `BackupServiceImpl` 实现（`library.backup.cron`，凌晨 2 点自动备份并清理 `keep-days` 之前的旧文件）。
- **配置参数**：业务参数集中在 [application.yml](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/resources/application.yml) 的 `library.*` 命名空间（默认借阅天数、最大续借次数、续借天数、逾期罚金、库存预警阈值、提醒/备份任务 cron 等），以 `@Value` 注入到 Service。

---

## 3. 数据模型与表关系

数据库脚本：[schema.sql](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/resources/db/schema.sql)，初始化数据：[data.sql](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/resources/db/data.sql)。

### 3.1 表清单

| 表名 | 含义 | 关键字段 |
| --- | --- | --- |
| `users` | 用户 | `id, username(uk), password, email, phone, avatar, status(0禁用/1启用), created_at, updated_at` |
| `roles` | 角色 | `id, role_name, role_code(uk), description` |
| `user_roles` | 用户-角色 多对多 | `user_id, role_id`，唯一索引 `(user_id, role_id)` |
| `permissions` | 权限/菜单 | `id, permission_name, permission_code(uk), resource_type(menu/button), parent_id, sort_order` |
| `role_permissions` | 角色-权限 多对多 | `role_id, permission_id`，唯一索引 `(role_id, permission_id)` |
| `categories` | 图书分类（自引用树） | `id, name, parent_id, sort_order` |
| `books` | 图书 | `id, isbn(uk), title, author, publisher, publish_date, price, category_id, cover_url, description, total_stock, available_stock, location, status(0下架/1上架)` |
| `borrow_records` | 借阅记录 | `id, user_id, book_id, borrow_date, due_date, return_date, renew_count, fine_amount, status(0借阅中/1已归还/2逾期), remark, created_at` |
| `notifications` | 站内通知 | `id, user_id, title, content, type(1系统/2到期/3逾期), is_read(0未读/1已读)` |
| `operation_logs` | 操作日志 | `id, user_id, username, operation, method, params, ip, time_cost, status(0失败/1成功), error_msg, created_at` |
| `system_config` | 系统配置 | `id, config_key(uk), config_value, description, updated_at` |

### 3.2 关键关系

```
users 1 ── * user_roles * ── 1 roles 1 ── * role_permissions * ── 1 permissions
                                                                       │
categories 1 ── * books 1 ── * borrow_records * ── 1 users           tree(parent_id)
                                                                  
notifications.user_id    →  users.id   (一对多，按用户站内通知)
operation_logs.user_id   →  users.id   (一对多，操作行为审计)
```

- `permissions.parent_id` / `categories.parent_id` 通过 `0` 表示根节点，自引用形成树状层级。
- `books.available_stock` 与 `books.total_stock` 的关系由借阅业务流程维护：借出 - 1，归还 + 1（见第 5 节）。
- 借阅记录的 `status` 与 `due_date / return_date` 联动：定时任务把"未归还且 due_date < now"的记录置为 2（逾期）。

### 3.3 初始化数据

- 内置 2 个角色：`ADMIN`、`USER`。
- 权限按"一级菜单 → 子菜单 → 按钮"三层结构编码（如 `book`、`book:list`、`book:create`）。
- 内置 3 个测试账号（密码统一为 BCrypt 加密的 `123456`）：`admin / user / test`，其中 `admin` 拥有全部权限，普通用户拥有图书查看、我的借阅及借/还/续借按钮权限。

---

## 4. 用户认证与权限控制

### 4.1 认证总流程

登录由 [AuthController.login](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/auth/controller/AuthController.java#L48-L54) 入口，[AuthServiceImpl.login](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/auth/service/impl/AuthServiceImpl.java#L160-L197) 完成核心逻辑，顺序为：

1. **图形验证码校验**：`verifyCaptcha(captchaKey, captchaCode)`，使用 `Hutool` 的 `LineCaptcha` 生成 Base64 图片，缓存在 `CAPTCHA_CACHE`，比较时不区分大小写，校验后立即从缓存移除（一次性使用）。
2. **加载用户**：`UserService.loadUserByUsername(username)`（[UserServiceImpl#loadUserByUsername](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/user/service/impl/UserServiceImpl.java#L45-L69)）一并查询 `roles` 与 `permissions`，构建 [LoginUser](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/LoginUser.java)（实现 `UserDetails`）。
3. **短信验证码校验**：登录场景必须以"用户名 → 绑定手机号"找到目标手机号，再校验 `smsCode`。注册场景直接使用入参 `phone`。验证码使用 `ConcurrentHashMap` 内存缓存，60 秒频率限制（`SMS_SEND_TIME_CACHE`），开发环境直接打印日志并把验证码原文返回前端方便调试（见 [AuthServiceImpl#sendSmsCode](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/auth/service/impl/AuthServiceImpl.java#L86-L123)）。
4. **密码校验**：`PasswordEncoder.matches(rawPassword, user.getPassword())`，使用 `BCryptPasswordEncoder`（[SecurityConfig#passwordEncoder](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/SecurityConfig.java#L95-L98)）。
5. **签发 Token**：[JwtTokenProvider](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/jwt/JwtTokenProvider.java) 基于 `jjwt 0.12.x` API（`Jwts.builder().claims(...).subject(...).signWith(getSigningKey())`），生成 `accessToken`（24h）+ `refreshToken`（7d）。Token 中携带 `userId / username / roles / permissions`。
6. **返回 [LoginVO](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/auth/vo/LoginVO.java)** 给前端，前端写入 [stores/user.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/src/stores/user.ts) 并持久化到 `localStorage`。

注册流程（[AuthServiceImpl#register](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/auth/service/impl/AuthServiceImpl.java#L208-L244)）依次校验图形验证码 → 短信验证码 → 两次密码一致 → 用户名/邮箱唯一性，然后调用 `UserService.createUser`，并默认分配 `roleId=2`（普通用户）。

刷新 Token：使用 `refreshToken` 解析用户名后重新签发 access/refresh 双 Token；登出接口仅返回成功，由前端清除 Token（JWT 无服务端会话）。

### 4.2 Spring Security 配置

- [SecurityConfig](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/SecurityConfig.java)：
  - 关闭 CSRF、关闭 Session（`SessionCreationPolicy.STATELESS`）。
  - 设置安全响应头（XSS、`X-Content-Type-Options`、`frame-options=sameOrigin`、CSP）。
  - 异常处理器：未登录走 [JwtAuthenticationEntryPoint](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/JwtAuthenticationEntryPoint.java) 返回 401；权限不足走 [JwtAccessDeniedHandler](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/JwtAccessDeniedHandler.java) 返回 403。两者均序列化为统一的 `Result` JSON。
  - 白名单（[WHITE_LIST](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/SecurityConfig.java#L39-L53)）：登录、注册、发送短信、刷新 Token、登出、验证码、Swagger 资源、`/uploads/**`、`/error`，以及全部 `OPTIONS` 预检。
  - 在 `UsernamePasswordAuthenticationFilter` 之前插入 [JwtAuthenticationFilter](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/filter/JwtAuthenticationFilter.java)。
  - 启用 `@EnableMethodSecurity`，使方法级 `@PreAuthorize("hasRole('ADMIN')")` 生效。

### 4.3 JWT 过滤器与上下文

[JwtAuthenticationFilter](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/filter/JwtAuthenticationFilter.java#L42-L87)：

1. 从 `Authorization: Bearer <token>` 头提取 Token。
2. 通过 `JwtTokenProvider.validateToken` 校验签名与过期。
3. 解析出 `userId / username / roles / permissions`，构建 `LoginUser`。
4. 通过 `UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())` 写入 `SecurityContextHolder`。

[LoginUser#getAuthorities](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/security/LoginUser.java#L77-L92) 把 `roles` 拼成 `ROLE_<roleCode>`（用于 `hasRole(...)`），把 `permissions` 直接作为 `SimpleGrantedAuthority`（可用于按钮级权限）。

业务侧通过 [SecurityUtil](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/util/SecurityUtil.java) 静态方法获取当前用户：`getCurrentUserId() / getCurrentUsername() / isAdmin()`。

### 4.4 权限控制方式

- **接口级**：在 Controller 方法上使用 `@PreAuthorize("hasRole('ADMIN')")`，例如借阅管理员接口、图书写操作接口、系统管理接口。普通用户只能访问"我的借阅、借书、还书、续借、检查可借阅"等接口。
- **菜单级（前端）**：路由表 [router/index.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/src/router/index.ts) 在 `meta.roles` 中声明可访问角色，由全局 `beforeEach` 守卫匹配 `userStore.roles`，无权限重定向 `/403`。
- **按钮级（前端）**：通过 `useUserStore().hasPermission(code)`（基于 `permissions[]`，`ADMIN` 角色直接放行）控制 UI 显隐。
- **数据所有权**：用户接口默认基于 `SecurityUtil.getCurrentUserId()` 过滤，例如"我的借阅"、"当前借阅"，避免越权访问他人记录。

---

## 5. 借阅业务状态流转

借阅是系统的核心域。完整实现集中在 [BorrowController](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/borrow/controller/BorrowController.java) 与 [BorrowServiceImpl](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java)。

### 5.1 状态机

`borrow_records.status` 取值：

| 值 | 含义 | 主要触发动作 |
| --- | --- | --- |
| `0` | 借阅中 | 创建借阅记录后初始状态 |
| `1` | 已归还 | `returnBook` 执行成功 |
| `2` | 逾期 | 定时任务 `updateOverdueStatus` 把未归还且超过 `due_date` 的记录置为 2 |

状态迁移关系：

```
              borrowBook
               (检查通过)
               扣减库存
                  ▼
       ┌────────────────┐
       │   0 借阅中     │────────renewBook(<=maxRenewTimes 且未逾期)──┐
       └──┬───────────┬─┘                                             │
          │           │                                               ▼
returnBook│           │ 定时任务 updateOverdueStatus       (dueDate顺延 renewDays)
(now>dueDate           │ now > due_date && status=0
计算罚款)              ▼
          │      ┌──────────┐  returnBook   ┌──────────┐
          │      │ 2 逾期   │──────────────►│ 1 已归还 │
          │      └──────────┘  (计算罚款)   └──────────┘
          ▼
     ┌──────────┐
     │ 1 已归还 │
     └──────────┘
```

### 5.2 借书 `borrowBook(userId, bookId)`

实现：[BorrowServiceImpl#borrowBook](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L98-L148)。

校验顺序与对应错误码：

1. 图书存在：否则 `BOOK_NOT_EXIST(3001)`。
2. 图书状态为"上架"（`status==1`）：否则 `DATA_ERROR(1004)` "该图书已下架"。
3. 可用库存 > 0：否则 `BOOK_STOCK_NOT_ENOUGH(3002)`。
4. 用户对该图书不存在借阅中记录：否则 `BOOK_ALREADY_BORROWED(4003)`。
5. 用户当前借阅总数 < `MAX_BORROW_COUNT(=5)`：否则 `BORROW_LIMIT_EXCEEDED(4001)`。
6. `BookService.decreaseStock(bookId, 1)` 必须成功：否则 `BOOK_STOCK_NOT_ENOUGH`（兜底防止并发条件下减库失败）。

成功后写入 `borrow_records`：`borrowDate = now`、`dueDate = now + library.default-borrow-days`（默认 30 天）、`renewCount = 0`、`fineAmount = 0`、`status = 0`。整个方法位于 `@Transactional(rollbackFor = Exception.class)` 事务内。

接口入口：
- 用户自助借阅 `POST /api/borrows`（请求体 `{ bookId }`）使用 `SecurityUtil.getCurrentUserId()` 获取借阅人。
- 管理员代借 `POST /api/borrows/admin`（请求体 `{ userId, bookId }`），需要 `ADMIN` 角色。

### 5.3 还书 `returnBook(borrowId)`

实现：[BorrowServiceImpl#returnBook](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L151-L180)。

1. 借阅记录必须存在：否则 `BORROW_RECORD_NOT_EXIST(4002)`。
2. `status != 1`（不能重复归还）：否则 `DATA_ERROR` "该图书已归还"。
3. 计算逾期罚款：若 `now > dueDate`，按天累计 `overdueDays * library.overdue-fine-per-day`（默认 0.5 元/天）。
4. 通过 `BorrowRecordMapper.updateReturn(id, returnDate, fineAmount, status=1)` 标记归还。
5. `BookService.increaseStock(bookId, 1)` 把可用库存补回。

事务边界确保"标记归还"与"恢复库存"原子提交。

### 5.4 续借 `renewBook(borrowId)`

实现：[BorrowServiceImpl#renewBook](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L183-L210)。

1. 记录存在：否则 `BORROW_RECORD_NOT_EXIST`。
2. `status != 1`：已归还无需续借（`BOOK_NOT_RETURNED`）。
3. `renewCount < library.max-renew-times`（默认 1 次）：否则 `RENEW_LIMIT_EXCEEDED(4004)`。
4. 不能在逾期状态下续借（`status==2 || now > dueDate`）：否则 `BORROW_OVERDUE(4006)`，需先归还。
5. `dueDate += library.renew-days`（默认 30 天），`renewCount + 1`，写库。

### 5.5 逾期处理

- **状态自动迁移**：定时任务 `BorrowServiceImpl.updateOverdueStatus`（cron `0 0 1 * * ?`）调用 `BorrowRecordMapper.updateOverdueStatus(now)`，把未归还且 `due_date < now` 的记录置为 `status=2`。统计接口在调用前也会先触发该方法以保证数据准确。
- **罚款计算**：在 `returnBook` 时一次性按 `now - dueDate` 的天数 × 单日罚金计算（不在状态迁移时预先写入），保证罚款随实际归还时点变动。
- **到期/逾期通知**：
  - 站内通知：`sendDueReminder`（cron `0 0 9 * * ?`）查询 `now ~ now + library.reminder-days-before` 内到期的记录，写入 `notifications` 表（`type=2`）。
  - 邮件通知：[BorrowReminderTask.sendBorrowReminders](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/task/BorrowReminderTask.java#L48-L78) 与 [sendOverdueNotices](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/task/BorrowReminderTask.java#L83-L110) 通过 `MailService` 分别发送到期提醒和逾期通知，邮件中包含图书名、应还日期、剩余/逾期天数与预计罚款。
- **逾期列表查询**：`GET /api/borrows/overdue` 仅 `ADMIN` 可见，分页返回当前所有 `status=2` 的记录。

### 5.6 状态-接口对照

| 接口 | 方法 | 路径 | 鉴权 | 触发的状态变化 |
| --- | --- | --- | --- | --- |
| 借阅图书 | POST | `/api/borrows` | 登录用户 | 新增 `status=0` |
| 管理员代借 | POST | `/api/borrows/admin` | `ADMIN` | 新增 `status=0` |
| 归还图书 | PUT | `/api/borrows/{id}/return` | 登录用户 | `status: 0/2 → 1` |
| 续借图书 | PUT | `/api/borrows/{id}/renew` | 登录用户 | `status=0`，`dueDate += renewDays`，`renewCount+1` |
| 我的借阅 | GET | `/api/borrows/my` | 登录用户 | 不变 |
| 当前借阅 | GET | `/api/borrows/current` | 登录用户 | 不变 |
| 借阅列表 | GET | `/api/borrows` | `ADMIN` | 不变 |
| 逾期列表 | GET | `/api/borrows/overdue` | `ADMIN` | 不变 |
| 借阅统计 | GET | `/api/borrows/statistics` | `ADMIN` | 内部触发 `updateOverdueStatus` |
| 是否可借 | GET | `/api/borrows/can-borrow` | 登录用户 | 不变 |

---

## 6. 前端架构

### 6.1 工程结构

前端采用 Vite + Vue 3 + TS，关键目录（`frontend/src`）：

| 目录 | 职责 |
| --- | --- |
| `api/` | 所有后端接口的 TypeScript 封装（`auth/book/borrow/category/notification/role/system/backup/user`） |
| `layouts/` | 主布局 `MainLayout.vue`（侧边栏菜单 + 顶部 + 内容区） |
| `router/` | `vue-router` 路由表 + 全局守卫（[index.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/src/router/index.ts)） |
| `stores/` | Pinia 状态管理，目前主要是 `user`（登录态、Token、角色权限） |
| `utils/` | 通用工具：HTTP 请求封装 [request.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/src/utils/request.ts)、`download / format / validators` |
| `views/` | 各业务页面，按模块分子目录 |
| `styles/` | 全局样式（SCSS） |
| `main.ts` | 入口：注册 Pinia、Router、Element Plus 与所有图标 |

构建配置 [vite.config.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/vite.config.ts)：
- 开发环境通过 `server.proxy['/api']` 代理到 `http://localhost:9999`，避免本地跨域。
- `unplugin-auto-import` 自动引入 Vue/Pinia/Router 与 Element Plus 组件，减少样板。
- `manualChunks` 把 Element Plus 与 Vue 全家桶分包以减小首包体积。

### 6.2 路由与权限

- 公开路由：`/login`、`/register`、`/403`、`/:pathMatch(.*)*`（404）。
- 业务路由全部挂在 `MainLayout` 子路由下，`/` 默认重定向到 `/books`。
- 路由 meta：
  - `requiresAuth`：默认 `true`，登录页/注册页/错误页显式置为 `false`。
  - `roles`：限制可访问角色，例如 `dashboard / users / roles / logs / config / backups / overdue / borrows / stock-warning` 都要求 `ADMIN`。
  - `hidden`：从侧边栏菜单中隐藏（用于详情/表单类二级页面）。
- 全局守卫 `router.beforeEach`：
  1. NProgress 进度条 + 设置标题。
  2. 已登录访问 `/login` 或 `/register` → 重定向 `/`。
  3. 需要登录但无 Token → 跳 `/login?redirect=...`。
  4. 检查 `meta.roles` 与 `userStore.roles` 是否相交，否则 → `/403`。

### 6.3 状态管理（Pinia）

[stores/user.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/src/stores/user.ts) 集中管理登录状态：
- `state`：`token / refreshToken / userId / username / email / avatar / roles[] / permissions[]`，初始化时从 `localStorage` 还原。
- `getters`：`isLoggedIn / isAdmin / hasPermission(code)`。
- `actions`：`login / setUserInfo / logout / clearUserInfo / updateAvatar`。`setUserInfo` 在写入状态的同时持久化到 `localStorage`，保证刷新页面后仍处于登录态。

### 6.4 HTTP 请求封装

[utils/request.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/src/utils/request.ts)：
- Axios 实例 `baseURL: '/api'`，`timeout: 30s`。
- 请求拦截器：除 `AUTH_WHITE_LIST`（登录/注册/验证码/短信/刷新 Token）外，自动从 `userStore.token` 取 Token 加入 `Authorization: Bearer ...`。
- 响应拦截器（业务层）：
  - `responseType=blob` 直接返回（用于下载）。
  - `code === 200` 返回 `res`；其他业务错误码统一 `ElMessage.error`。
- 响应拦截器（HTTP 错误层）：
  - `401`：在登录/注册页面静默；其他页面用 `ElMessageBox.confirm` 弹出"登录已过期，请重新登录"，用户确认后调用 `userStore.logout()` 并跳转 `/login`。使用 `isShowingLogoutDialog` 标记防止并发 401 重复弹窗。
  - `403 / 404 / 500`：分别 `ElMessage.error`。
  - `ECONNABORTED`：超时提示。
  - 其他：网络连接失败提示。
- 暴露 `request.get/post/put/delete/upload`，`upload` 自动构造 `multipart/form-data`。

### 6.5 视图层关键页面

- **登录/注册** `views/login/`：图形验证码 + 短信验证码 + 用户名 + 密码联合校验。
- **图书管理** `views/book/`：列表（分页/搜索/筛选）、表单（新增/编辑/封面上传/Excel 导入/导出）、库存预警（管理员）。
- **借阅** `views/borrow/`：`BorrowList`（管理员看全部）、`MyBorrows`（用户看自己，按 Tab 切换"全部/借阅中/已归还"，提供"归还/续借"按钮，根据 `renewCount >= 1 || status === 2` 自动禁用续借）、`OverdueList`（管理员逾期列表）。
- **用户/角色/分类/通知/系统配置/操作日志/数据备份**：分别对应后端模块，权限根据路由 meta 控制。
- **错误页** `views/error/403.vue`、`404.vue`。

---

## 7. 前后端关键接口与异常处理

### 7.1 接口分布概览

后端所有接口均位于 `/api` 前缀下，按模块组织 RESTful 路由：

| 模块 | 路径前缀 | 主要接口（节选） |
| --- | --- | --- |
| 认证 | `/api/auth` | `POST /login`, `POST /register`, `POST /send-sms-code`, `GET /captcha`, `POST /refresh-token`, `POST /logout` |
| 图书 | `/api/books` | `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/stock`, `GET /stock-warning`, `POST /cover/upload`, `POST /import`, `POST /import/excel`, `GET /import/template`, `GET /export`, `GET /export/excel`, `PUT /{id}/status` |
| 借阅 | `/api/borrows` | `GET /`, `GET /{id}`, `POST /`, `POST /admin`, `PUT /{id}/return`, `PUT /{id}/renew`, `GET /my`, `GET /current`, `GET /overdue`, `GET /statistics`, `GET /can-borrow` |
| 用户 | `/api/users` | 用户 CRUD、状态切换、修改密码、重置密码、角色分配 |
| 角色 | `/api/roles` | 角色 CRUD、权限分配 |
| 分类 | `/api/categories` | 分类树 CRUD |
| 通知 | `/api/notifications` | 列表、未读数、标记已读、全部已读 |
| 系统 | `/api/system` | 操作日志、系统配置；备份接口在 `BackupServiceImpl` |

接口文档由 SpringDoc 自动生成：`http://localhost:9999/api/swagger-ui.html`。

### 7.2 前后端契约

- 请求体一律使用 JSON，文件上传使用 `multipart/form-data`，下载使用 `responseType=blob`。
- 响应统一为 [Result](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/response/Result.java)：`{ code, message, data, timestamp }`。
- 分页响应封装为 `Result<PageResult<T>>`，`PageResult` 自带 `pages / hasPrevious / hasNext` 计算（[PageResult.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/response/PageResult.java)）。
- 前端 API 层用 TypeScript 接口表达数据形状（如 [api/borrow.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/src/api/borrow.ts) 中的 `BorrowRecord / BorrowQueryParams / BorrowStatistics`）。

### 7.3 异常处理策略

整体策略是"业务错误用 `Result.code` 表达，HTTP 状态码区分认证/权限/服务器异常"。

**后端**（[GlobalExceptionHandler](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/backend/src/main/java/com/library/common/exception/GlobalExceptionHandler.java)）：

| 异常类型 | HTTP 状态 | 返回 code | 处理策略 |
| --- | --- | --- | --- |
| `BusinessException` | 200（HTTP）+ 自定义 code | 例如 4001/4003/3002 等 | 业务可控错误，由前端读 `code/message` 提示 |
| `MethodArgumentNotValidException` 等参数校验 | 400 | 1001 | 拼接字段与默认信息 |
| `MissingServletRequestParameterException` / `MethodArgumentTypeMismatchException` / `HttpMessageNotReadableException` | 400 | 1001 | 描述具体问题字段 |
| `HttpRequestMethodNotSupportedException` | 405 | 405 | 标识请求方法不允许 |
| `NoHandlerFoundException` | 404 | 404 | 接口不存在 |
| `MaxUploadSizeExceededException` | 400 | 1001 | 文件超限 |
| `AuthenticationException` / `BadCredentialsException` | 401 | 401 / 2002 | 未认证或密码错误 |
| `AccessDeniedException` | 403 | 403 | 权限不足 |
| 其他 `Exception` | 500 | 500 | 兜底"系统繁忙，请稍后重试"，写 ERROR 日志 |

Spring Security 自身的鉴权异常会先被 `JwtAuthenticationEntryPoint`（401）和 `JwtAccessDeniedHandler`（403）拦截，直接写出 `Result` JSON，不会走到 `GlobalExceptionHandler`。

**前端**（[utils/request.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/src/utils/request.ts)）：

- 业务错误（HTTP 200 但 `code !== 200`）：`ElMessage.error(message)` 并 `Promise.reject`，由调用方决定是否再做处理。
- HTTP 状态错误：在响应错误拦截器中按 `status` 分支统一提示；401 走 `ElMessageBox` 强制重新登录。
- 网络/超时错误：超时和无响应分别提示"请求超时""网络连接失败"。
- 业务页面里通常以 `try/catch` 包裹 API 调用，仅处理需要业务感知的失败逻辑（如重置表单、保留模态框打开），其他情况依赖拦截器统一弹窗。

### 7.4 文件上传与下载

- **上传**：图书封面 `POST /api/books/cover/upload` 走 `multipart/form-data`，存盘到 `${file.upload.path}/covers/yyyy/MM/dd/<uuid>.jpg`，并通过 `WebMvcConfig` 的 `addResourceHandlers` 把 `/uploads/**` 映射到本地，前端可直接 `<img :src="/uploads/...">` 预览。Nginx 同样代理 `/uploads`（[nginx.conf#L33-L37](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-summer/frontend/nginx.conf#L33-L37)）。
- **下载**：Excel 导出（如 `GET /api/books/export/excel`）由后端直接写出二进制流并设置 `Content-Disposition`；前端使用 `responseType: 'blob'`，由响应拦截器原样返回响应对象。

### 7.5 跨域与代理

- **开发环境**：前端 8081 → Vite dev server proxy → 后端 9999。
- **生产/容器环境**：浏览器 → Nginx 80 (映射宿主 8081) → `proxy_pass http://backend:9999`。
- 后端同时通过 `WebMvcConfig.addCorsMappings` 配置了 CORS 兜底，避免直连场景下的预检失败。

---

## 8. 总结要点

1. **分层清晰**：后端遵循 `controller / service / mapper / entity / dto / vo` 的传统五层（含通用 `BaseEntity / BaseMapper / BaseService(Impl)`），所有业务模块结构高度一致；前端按 `api / stores / router / views / utils / layouts` 解耦，UI 与状态、网络分离。
2. **认证以 JWT 为核心**：登录通过"图形验证码 + 短信验证码 + 用户名密码"三段校验，签发 access/refresh 双 Token；过滤器解析 Token 写入 `SecurityContextHolder`，方法级 `@PreAuthorize` + 路由 `meta.roles` + 按钮级 `permissions` 共同构成多层鉴权。
3. **统一响应、集中异常**：`Result<T> + ResultCode + BusinessException + GlobalExceptionHandler` 让"业务错误码"与"HTTP 异常"各司其职，前端拦截器再做兜底提示，错误链路闭合可控。
4. **借阅业务是核心域**：借/还/续借三个原子操作均在 Service 层加事务，库存通过 `decreaseStock / increaseStock` 加减；逾期由"定时迁移状态 + 还书时点计算罚款"两条路径协同；到期/逾期分别通过站内通知与邮件双通道触达。
5. **可运维特性**：`@OperationLog` 注解 + AOP 实现自动审计；`BackupService` 支持定时数据库备份与按天清理；`library.*` 配置项把借阅、提醒、备份等业务参数外置于 `application.yml`，便于环境差异化。
6. **架构边界稳定**：项目采用前后端分离 + Nginx 反向代理的部署模式，前端依赖 `localStorage` 维持会话，后端无状态化（JWT），便于水平扩展。本文档严格按照该既有架构与技术栈进行梳理，未引入额外组件或重构建议。
