# 图书管理系统技术架构文档

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈总览](#2-技术栈总览)
3. [后端架构设计](#3-后端架构设计)
4. [前端架构设计](#4-前端架构设计)
5. [核心数据模型](#5-核心数据模型)
6. [用户认证与权限控制](#6-用户认证与权限控制)
7. [借阅业务流程](#7-借阅业务流程)
8. [前后端交互规范](#8-前后端交互规范)
9. [异常处理策略](#9-异常处理策略)
10. [定时任务与系统服务](#10-定时任务与系统服务)

---

## 1. 项目概述

本项目是一个前后端分离的图书管理系统，提供图书管理、用户借阅、权限控制、消息通知、系统配置等完整功能。系统采用RBAC权限模型，支持多角色（管理员、普通用户），具备完善的业务规则校验和异常处理机制。

### 核心特性

- 用户注册登录（图形验证码 + 短信验证码双重验证）
- JWT无状态认证（accessToken + refreshToken双令牌机制）
- 图书分类管理、库存管理、批量导入导出
- 借阅全流程管理（借书、还书、续借、逾期处理、罚款计算）
- 站内通知与到期提醒
- 操作日志审计
- 数据备份功能

---

## 2. 技术栈总览

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 应用框架 |
| Spring Security | 6.x | 安全认证框架 |
| Spring AOP | - | 切面编程（操作日志） |
| Spring Scheduling | - | 定时任务 |
| MyBatis | 3.0.3 | ORM框架 |
| MySQL | 8.x | 关系型数据库 |
| JWT (jjwt) | 0.12.3 | Token生成与验证 |
| Hutool | 5.8.23 | Java工具类库 |
| EasyExcel | 3.3.3 | Excel导入导出 |
| SpringDoc OpenAPI | 2.3.0 | API文档生成 |
| Lombok | - | 代码简化 |
| H2 Database | - | 测试环境数据库 |

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.15 | 前端框架 |
| TypeScript | 5.6.0 | 类型系统 |
| Vite | 5.0.11 | 构建工具 |
| Vue Router | 4.2.5 | 路由管理 |
| Pinia | 2.1.7 | 状态管理 |
| Element Plus | 2.5.1 | UI组件库 |
| Axios | 1.6.5 | HTTP客户端 |
| Day.js | 1.11.10 | 日期处理 |
| NProgress | 0.2.0 | 进度条 |
| Sass | 1.70.0 | CSS预处理 |

---

## 3. 后端架构设计

### 3.1 目录结构

```
backend/src/main/java/com/library/
├── LibraryApplication.java          # 启动类
├── aspect/                          # AOP切面
│   ├── OperationLog.java            # 操作日志注解
│   └── OperationLogAspect.java      # 操作日志切面实现
├── common/                          # 公共模块
│   ├── base/                        # 基础类（Entity/Mapper/Service）
│   │   ├── BaseEntity.java
│   │   ├── BaseMapper.java
│   │   ├── BaseService.java
│   │   └── BaseServiceImpl.java
│   ├── config/                      # 配置类
│   │   ├── MailConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── WebMvcConfig.java
│   ├── exception/                   # 异常处理
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── response/                    # 响应封装
│   │   ├── PageResult.java
│   │   ├── Result.java
│   │   └── ResultCode.java
│   ├── service/                     # 公共服务（邮件）
│   ├── util/                        # 工具类（IP工具、安全工具）
│   └── xss/                         # XSS防护
├── module/                          # 业务模块（按领域划分）
│   ├── auth/                        # 认证模块
│   ├── book/                        # 图书模块
│   ├── borrow/                      # 借阅模块
│   ├── category/                    # 分类模块
│   ├── notification/                # 通知模块
│   ├── role/                        # 角色权限模块
│   ├── system/                      # 系统模块（日志、配置、备份）
│   └── user/                        # 用户模块
├── security/                        # 安全模块
│   ├── filter/JwtAuthenticationFilter.java
│   ├── jwt/JwtTokenProvider.java
│   ├── JwtAccessDeniedHandler.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── LoginUser.java
│   └── SecurityConfig.java
└── task/                            # 定时任务
    └── BorrowReminderTask.java
```

### 3.2 分层设计

后端采用经典的三层架构，每个业务模块内部遵循统一的分层规范：

| 层次 | 包名 | 职责 |
|------|------|------|
| Controller层 | `controller/` | 接收HTTP请求，参数校验，调用Service，返回响应 |
| Service层 | `service/` | 业务逻辑处理，事务控制，调用Mapper |
| Mapper层 | `mapper/` | 数据访问，SQL执行，对应resources/mapper下的XML文件 |
| Entity | `entity/` | 数据库实体类，与表结构对应 |
| DTO | `dto/` | 数据传输对象，接收前端请求参数 |
| VO | `vo/` | 视图对象，返回给前端的数据结构 |

### 3.3 基类设计

系统通过泛型基类实现通用CRUD的复用：

- [BaseEntity.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/base/BaseEntity.java)：所有实体的父类，包含`id`、`createdAt`、`updatedAt`公共字段

- [BaseMapper.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/base/BaseMapper.java)：通用Mapper接口，定义基础CRUD方法

- [BaseService.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/base/BaseService.java)：通用Service接口

- [BaseServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/base/BaseServiceImpl.java)：通用Service实现，提供分页查询、批量操作等基础实现，代码位置：

```java
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T> implements BaseService<T> {
    @Autowired
    protected M baseMapper;

    @Override
    public PageResult<T> page(Map<String, Object> params, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<T> list = baseMapper.selectPage(params, offset, pageSize);
        long total = baseMapper.countByCondition(params);
        return PageResult.of(list, total, pageNum, pageSize);
    }
    // ... 其他通用方法
}
```

### 3.4 业务模块划分

| 模块 | 核心功能 | 主要文件 |
|------|----------|----------|
| **auth** | 登录、注册、验证码、Token刷新 | [AuthController.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/auth/controller/AuthController.java), [AuthServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/auth/service/impl/AuthServiceImpl.java) |
| **user** | 用户CRUD、角色分配、密码管理 | [UserServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/user/service/impl/UserServiceImpl.java) |
| **role** | 角色CRUD、权限管理 | role/ |
| **book** | 图书CRUD、分类关联、库存管理、导入导出 | book/ |
| **borrow** | 借阅核心业务：借书、还书、续借、逾期处理 | [BorrowServiceImpl.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java) |
| **category** | 图书分类树形管理 | category/ |
| **notification** | 站内通知 | notification/ |
| **system** | 操作日志、系统配置、数据备份 | system/ |

---

## 4. 前端架构设计

### 4.1 目录结构

```
frontend/src/
├── api/                      # API接口层（按模块划分）
│   ├── auth.ts
│   ├── book.ts
│   ├── borrow.ts
│   ├── category.ts
│   ├── notification.ts
│   ├── role.ts
│   ├── system.ts
│   └── user.ts
├── layouts/                  # 布局组件
│   └── MainLayout.vue        # 主布局（侧边栏+顶部+内容区）
├── router/                   # 路由配置
│   └── index.ts
├── stores/                   # Pinia状态管理
│   └── user.ts               # 用户状态Store
├── styles/                   # 全局样式
├── utils/                    # 工具函数
│   ├── request.ts            # Axios封装
│   ├── download.ts           # 文件下载
│   ├── format.ts             # 格式化工具
│   └── validators.ts         # 验证工具
├── views/                    # 页面组件
│   ├── book/                 # 图书相关页面
│   ├── borrow/               # 借阅相关页面
│   ├── category/             # 分类管理
│   ├── dashboard/            # 仪表盘
│   ├── error/                # 错误页面
│   ├── login/                # 登录注册
│   ├── notification/         # 通知中心
│   ├── role/                 # 角色管理
│   ├── system/               # 系统管理
│   └── user/                 # 用户中心
├── App.vue
└── main.ts                   # 应用入口
```

### 4.2 前端核心流程

#### 入口初始化
[main.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/frontend/src/main.ts) 完成Vue应用初始化，注册Pinia、Router、Element Plus及图标组件。

#### 路由守卫
[index.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/frontend/src/router/index.ts) 实现全局路由守卫：
- 检查登录状态（Token是否存在）
- 未登录跳转登录页，携带redirect参数
- 检查页面角色权限（meta.roles），无权限跳转403页
- 已登录用户访问登录/注册页自动重定向到首页
- NProgress进度条控制

#### 状态管理
[user.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/frontend/src/stores/user.ts) 使用Pinia管理用户状态：
- Token持久化到localStorage，页面刷新自动恢复
- 提供isAdmin、hasPermission等计算属性用于权限判断
- 封装login、logout、setUserInfo等action

---

## 5. 核心数据模型

### 5.1 ER图关系

```
users (用户表)
  ├── 1:N ── user_roles ── N:1 ── roles (角色表)
  │                              └── 1:N ── role_permissions ── N:1 ── permissions (权限表)
  ├── 1:N ── borrow_records (借阅记录)
  │              └── N:1 ── books (图书表)
  │                            └── N:1 ── categories (分类表)
  └── 1:N ── notifications (通知表)

operation_logs (操作日志表)  [独立表，通过user_id关联]
system_config (系统配置表)    [独立KV表]
```

### 5.2 核心表结构

#### users 用户表
[schema.sql#L15-L28](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/resources/db/schema.sql#L15-L28)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| password | VARCHAR(100) | BCrypt加密密码 |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(255) | 头像URL |
| status | TINYINT | 状态：0禁用，1启用 |
| created_at / updated_at | DATETIME | 时间戳 |

#### roles 角色表 / permissions 权限表
- 角色：ADMIN（管理员）、USER（普通用户）
- 权限通过`role_permissions`关联表分配给角色，支持菜单权限和按钮权限两类资源类型

#### books 图书表
[schema.sql#L103-L125](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/resources/db/schema.sql#L103-L125)

| 字段 | 类型 | 说明 |
|------|------|------|
| isbn | VARCHAR(20) | ISBN，唯一 |
| title / author / publisher | VARCHAR | 书名、作者、出版社 |
| category_id | BIGINT | 关联分类ID |
| total_stock | INT | 总库存 |
| available_stock | INT | 可用库存（借阅时扣减，归还时增加） |
| status | TINYINT | 0下架，1上架 |

#### borrow_records 借阅记录表
[schema.sql#L131-L148](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/resources/db/schema.sql#L131-L148)

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | BIGINT | 借阅用户ID |
| book_id | BIGINT | 图书ID |
| borrow_date | DATETIME | 借阅日期 |
| due_date | DATETIME | 应还日期 |
| return_date | DATETIME | 实际归还日期（空表示未还） |
| renew_count | INT | 续借次数 |
| fine_amount | DECIMAL | 逾期罚款金额 |
| status | TINYINT | **0借阅中，1已归还，2逾期** |

#### notifications 通知表
| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | BIGINT | 接收用户ID |
| type | TINYINT | 1系统通知，2到期提醒，3逾期通知 |
| is_read | TINYINT | 0未读，1已读 |

---

## 6. 用户认证与权限控制

### 6.1 认证架构

系统采用Spring Security + JWT实现无状态认证，整体流程如下：

```
客户端                              服务器
  │                                   │
  ├─── POST /auth/login ────────────>│
  │    {username, password,          │  1. 校验图形验证码
  │     captchaKey, captchaCode,     │  2. 校验短信验证码
  │     smsCode}                     │  3. BCrypt验证密码
  │                                   │  4. 生成双Token返回
  │<── {accessToken, refreshToken} ──┤
  │                                   │
  ├─── GET /api/xxx ────────────────>│
  │    Authorization: Bearer xxx     │  JwtAuthenticationFilter
  │                                   │  1. 解析Token
  │                                   │  2. 验证签名和有效期
  │                                   │  3. 构建SecurityContext
  │<── 200 OK ───────────────────────┤
```

### 6.2 JWT实现细节

核心类：[JwtTokenProvider.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/security/jwt/JwtTokenProvider.java)

- **双Token机制**：
  - `accessToken`：有效期24小时，携带用户ID、用户名、角色、权限信息
  - `refreshToken`：有效期7天，仅携带用户ID和type标识，用于无感刷新
- **签名算法**：HMAC-SHA (jjwt 0.12.3)
- **配置项**（[application.yml#L50-L55](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/resources/application.yml#L50-L55)）：
  ```yaml
  jwt:
    secret: LibraryManagementSystemSecretKey2024...
    expiration: 86400000       # 24小时
    refresh-expiration: 604800000  # 7天
    header: Authorization
    prefix: "Bearer "
  ```

### 6.3 JWT认证过滤器

[JwtAuthenticationFilter.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/security/filter/JwtAuthenticationFilter.java) 继承OncePerRequestFilter，每次请求执行：

1. 从请求头提取Authorization字段，去除"Bearer "前缀获取Token
2. 调用`jwtTokenProvider.validateToken()`验证有效性
3. 从Token解析userId、username、roles、permissions
4. 构建LoginUser（实现UserDetails接口）
5. 创建UsernamePasswordAuthenticationToken写入SecurityContextHolder

### 6.4 Security安全配置

[SecurityConfig.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/security/SecurityConfig.java) 关键配置：

- **无状态Session**：`SessionCreationPolicy.STATELESS`，不创建HttpSession
- **CSRF禁用**：前后端分离架构，使用JWT无需CSRF防护
- **白名单路径**：登录、注册、验证码、Swagger文档、静态资源等无需认证
- **方法级安全**：`@EnableMethodSecurity`启用`@PreAuthorize`注解
- **密码编码器**：BCryptPasswordEncoder（强度10）
- **安全响应头**：启用XSS防护、X-Frame-Options、CSP策略

### 6.5 RBAC权限模型

**登录用户信息**：[LoginUser.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/security/LoginUser.java)

- 实现UserDetails接口，包含roles（Set<String>）和permissions（Set<String>）
- `getAuthorities()`合并角色前缀`ROLE_`和权限，返回Spring Security所需的GrantedAuthority集合

**权限控制方式**：

1. **URL级**：SecurityConfig的authorizeHttpRequests配置
2. **方法级注解**：Controller方法使用`@PreAuthorize`
   ```java
   @PreAuthorize("hasRole('ADMIN')")           // 需要ADMIN角色
   @PreAuthorize("hasAuthority('book:create')")  // 需要特定权限
   ```
3. **前端路由级**：router/index.ts的meta.roles配置
4. **前端菜单/按钮级**：通过userStore.hasPermission判断显示/隐藏

**初始化数据**：[data.sql](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/resources/db/data.sql)
- 预置角色：ADMIN（管理员）、USER（普通用户）
- 预置权限：按菜单/按钮分级编码（如book:create, book:update, borrow:return等）
- 预置账号：admin/123456（管理员）、user/123456（普通用户）

### 6.6 登录流程详解

[AuthServiceImpl.login()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/auth/service/impl/AuthServiceImpl.java#L159-L197)

1. **图形验证码校验**：从ConcurrentHashMap缓存取出验证码（一次性），不区分大小写比较
2. **短信验证码校验**：6位数字，60秒冷却期，校验后立即删除
3. **用户加载**：UserServiceImpl.loadUserByUsername()查询用户+角色+权限
4. **密码验证**：BCryptPasswordEncoder.matches()比对
5. **Token生成**：accessToken含完整权限信息，refreshToken用于刷新
6. **返回VO**：LoginVO包含token、过期时间、用户基本信息、角色权限列表

---

## 7. 借阅业务流程

### 7.1 借阅状态定义

借阅记录status字段：

| 值 | 状态 | 说明 |
|----|------|------|
| 0 | 借阅中 | 正常借阅，未到期 |
| 1 | 已归还 | 图书已归还 |
| 2 | 逾期 | 超过due_date未归还 |

### 7.2 借书流程

入口：[BorrowServiceImpl.borrowBook()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L99-L148)

```
开始
  │
  ▼
检查图书是否存在 ──不存在──> 抛出BOOK_NOT_EXIST
  │
  ▼
检查图书是否上架 ──已下架──> 抛出"该图书已下架"
  │
  ▼
检查available_stock > 0 ──无库存──> 抛出BOOK_STOCK_NOT_ENOUGH
  │
  ▼
查询是否已借阅同一本书 ──已借阅──> 抛出BOOK_ALREADY_BORROWED
  │
  ▼
统计当前借阅数量 ──>=5本──> 抛出BORROW_LIMIT_EXCEEDED（每人最多借5本）
  │
  ▼
扣减图书available_stock（事务内）
  │
  ▼
创建borrow_records记录：
  - borrow_date = 当前时间
  - due_date = 当前时间 + 30天（默认配置）
  - renew_count = 0
  - fine_amount = 0
  - status = 0
  │
  ▼
结束
```

**接口**：
- 普通用户借书：`POST /api/borrows` （从SecurityContext获取userId）
- 管理员代借：`POST /api/borrows/admin` （需ADMIN角色，指定userId）

### 7.3 还书流程

入口：[BorrowServiceImpl.returnBook()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L151-L180)

```
开始
  │
  ▼
检查借阅记录是否存在 ──不存在──> 抛出BORROW_RECORD_NOT_EXIST
  │
  ▼
检查是否已归还 ──status=1──> 抛出"该图书已归还"
  │
  ▼
判断是否逾期（now > due_date）
  │是
  ▼
计算逾期天数：ChronoUnit.DAYS.between(due_date, now)
计算罚款：overdueDays × 0.5元/天（配置项）
  │
  ▼
更新借阅记录：
  - return_date = 当前时间
  - fine_amount = 罚款金额
  - status = 1（已归还）
  │
  ▼
恢复图书available_stock +1（事务内）
  │
  ▼
结束
```

**接口**：`PUT /api/borrows/{id}/return`

### 7.4 续借流程

入口：[BorrowServiceImpl.renewBook()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L183-L210)

```
开始
  │
  ▼
检查借阅记录是否存在 ──不存在──> 抛出BORROW_RECORD_NOT_EXIST
  │
  ▼
检查是否已归还 ──已归还──> 抛出"该图书已归还，无需续借"
  │
  ▼
检查续借次数 ──>=maxRenewTimes(1)──> 抛出RENEW_LIMIT_EXCEEDED
  │
  ▼
检查是否逾期 ──status=2或已过due_date──> 抛出BORROW_OVERDUE（逾期需先归还）
  │
  ▼
更新：
  - due_date = 原due_date + 30天（renewDays配置）
  - renew_count = renew_count + 1
  │
  ▼
结束
```

**关键配置**（application.yml）：
- 每本书最多续借：1次
- 续借天数：30天
- 默认借阅期限：30天
- 逾期罚款：0.5元/天

### 7.5 逾期处理机制

**定时任务1 - 更新逾期状态**：[BorrowServiceImpl.updateOverdueStatus()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L241-L245)

```java
@Scheduled(cron = "0 0 1 * * ?")  // 每天凌晨1点执行
```
- 批量将due_date < 当前时间 且 status=0 的记录更新为status=2（逾期）

**定时任务2 - 到期提醒**：[BorrowServiceImpl.sendDueReminder()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/module/borrow/service/impl/BorrowServiceImpl.java#L248-L269)

```java
@Scheduled(cron = "0 0 9 * * ?")  // 每天上午9点执行
```
- 查询未来3天内到期的借阅记录（reminderDaysBefore配置）
- 为对应用户创建站内通知（type=2，到期提醒）

### 7.6 借阅业务规则汇总

| 规则 | 值 | 配置位置 |
|------|-----|----------|
| 每人最大借阅数量 | 5本 | MAX_BORROW_COUNT常量 |
| 默认借阅天数 | 30天 | library.default-borrow-days |
| 最大续借次数 | 1次 | library.max-renew-times |
| 续借延长天数 | 30天 | library.renew-days |
| 逾期日罚款 | 0.5元/天 | library.overdue-fine-per-day |
| 到期提前提醒天数 | 3天 | library.reminder-days-before |
| 逾期状态检查时间 | 每天凌晨1点 | @Scheduled cron |
| 到期提醒发送时间 | 每天上午9点 | @Scheduled cron |

---

## 8. 前后端交互规范

### 8.1 统一响应格式

后端所有接口返回统一JSON结构，定义在[Result.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/response/Result.java)：

```json
{
  "code": 200,           // 状态码，200表示成功
  "message": "操作成功", // 提示消息
  "data": {},            // 业务数据（对象、数组、分页结果）
  "timestamp": 1700000000000  // 服务器时间戳
}
```

### 8.2 分页响应格式

[PageResult.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/response/PageResult.java)：

```json
{
  "list": [],            // 数据列表
  "total": 100,          // 总记录数
  "pageNum": 1,          // 当前页码
  "pageSize": 10,        // 每页条数
  "totalPages": 10       // 总页数
}
```

### 8.3 API路径规范

- 后端context-path：`/api`（[application.yml#L4](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/resources/application.yml#L4)）
- 前端baseURL：`/api`（[request.ts#L20](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/frontend/src/utils/request.ts#L20)，通过Nginx代理到后端9999端口）
- RESTful风格：
  - GET：查询资源
  - POST：创建资源
  - PUT：更新资源
  - DELETE：删除资源

### 8.4 关键接口清单

#### 认证模块 `/api/auth`
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /auth/login | 用户登录 | 否 |
| POST | /auth/register | 用户注册 | 否 |
| POST | /auth/refresh-token | 刷新Token | 否 |
| GET | /auth/captcha | 获取图形验证码 | 否 |
| POST | /auth/send-sms-code | 发送短信验证码 | 否 |
| POST | /auth/logout | 退出登录 | 否 |

#### 图书模块 `/api/books`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /books | 分页查询图书 | 已认证 |
| GET | /books/{id} | 获取图书详情 | 已认证 |
| POST | /books | 新增图书 | ADMIN |
| PUT | /books/{id} | 更新图书 | ADMIN |
| DELETE | /books/{id} | 删除图书 | ADMIN |
| POST | /books/import | 批量导入 | ADMIN |
| GET | /books/export | 批量导出 | ADMIN |

#### 借阅模块 `/api/borrows`
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /borrows | 所有借阅记录（分页） | ADMIN |
| GET | /borrows/my | 我的借阅记录 | 已认证 |
| POST | /borrows | 借阅图书 | 已认证 |
| POST | /borrows/admin | 管理员代借 | ADMIN |
| PUT | /borrows/{id}/return | 归还图书 | 已认证 |
| PUT | /borrows/{id}/renew | 续借图书 | 已认证 |
| GET | /borrows/overdue | 逾期列表 | ADMIN |
| GET | /borrows/statistics | 借阅统计 | ADMIN |

### 8.5 前端请求封装

[request.ts](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/frontend/src/utils/request.ts) 核心机制：

1. **请求拦截器**：
   - 白名单接口（登录、注册、验证码）不带Token
   - 其他接口自动添加`Authorization: Bearer ${token}`头

2. **响应拦截器**：
   - code=200：正常返回data
   - 非200：ElMessage.error弹出错误消息
   - blob类型（文件下载）直接返回response

3. **HTTP状态码处理**：
   - 401：弹出确认框引导重新登录（防抖防止重复弹窗）
   - 403：提示"没有操作权限"
   - 404：提示"资源不存在"
   - 500：显示服务器错误消息
   - 超时/网络错误：友好提示

### 8.6 获取当前登录用户

后端工具类：SecurityUtil.getCurrentUserId()从SecurityContextHolder获取LoginUser，再取userId。

---

## 9. 异常处理策略

### 9.1 异常体系架构

```
BusinessException (业务异常)
    └── 携带code和message，用于业务规则校验失败

Spring/Java内置异常
    ├── MethodArgumentNotValidException  (RequestBody参数校验失败)
    ├── ConstraintViolationException     (RequestParam/PathVariable校验失败)
    ├── BindException                    (参数绑定失败)
    ├── HttpRequestMethodNotSupportedException (请求方法错误)
    ├── MissingServletRequestParameterException (缺少参数)
    ├── MaxUploadSizeExceededException   (文件过大)
    ├── AuthenticationException          (认证失败，如密码错误)
    ├── AccessDeniedException            (权限不足)
    └── Exception                        (兜底：其他未知异常)
```

### 9.2 全局异常处理器

[GlobalExceptionHandler.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/exception/GlobalExceptionHandler.java) 使用`@RestControllerAdvice`统一捕获：

- **业务异常**：记录WARN日志，返回自定义code和message（前端直接显示）
- **参数校验异常**：拼接所有字段错误（如"username: 不能为空; password: 长度至少6位"）
- **认证/授权异常**：返回401/403状态码及对应消息
- **未知异常**：记录ERROR日志+堆栈，返回"系统繁忙，请稍后重试"，不暴露内部错误

### 9.3 业务错误码

定义在[ResultCode.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/response/ResultCode.java)，分段管理：

| 编码段 | 含义 |
|--------|------|
| 200 | 成功 |
| 400-409, 500-503 | HTTP标准状态码 |
| 1xxx | 通用业务错误（参数错误、数据不存在等） |
| 2xxx | 用户模块（用户不存在、密码错误、验证码错误、短信相关） |
| 3xxx | 图书模块（图书不存在、库存不足、ISBN重复） |
| 4xxx | 借阅模块（借阅上限、已借阅、续借上限、逾期等） |

**典型借阅业务错误码**：
- 4001 BORROW_LIMIT_EXCEEDED：借阅数量已达上限（5本）
- 4002 BORROW_RECORD_NOT_EXIST：借阅记录不存在
- 4003 BOOK_ALREADY_BORROWED：该图书已被借阅（不能重复借）
- 4004 RENEW_LIMIT_EXCEEDED：续借次数已达上限
- 4006 BORROW_OVERDUE：借阅已逾期，请先归还

### 9.4 XSS防护

后端提供XSS防护机制：
- [XssFilter.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/xss/XssFilter.java)：全局Filter
- [XssHttpServletRequestWrapper.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/common/xss/XssHttpServletRequestWrapper.java)：包装请求，对参数进行HTML转义
- 配合Spring Security的CSP和XXssProtection头

---

## 10. 定时任务与系统服务

### 10.1 定时任务汇总

| 任务 | Cron表达式 | 执行时间 | 功能 |
|------|------------|----------|------|
| updateOverdueStatus | `0 0 1 * * ?` | 每天01:00 | 扫描并标记逾期借阅记录（status=2） |
| sendDueReminder | `0 0 9 * * ?` | 每天09:00 | 给3天内到期的用户发送站内通知 |

定时任务通过`@EnableScheduling`启用，在[LibraryApplication.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/LibraryApplication.java)配置。

### 10.2 操作日志AOP

[OperationLogAspect.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0608/source%20code/app-01231/app-01231-autumn/backend/src/main/java/com/library/aspect/OperationLogAspect.java) 通过AOP自动记录操作日志：

- 注解：`@OperationLog(value = "操作描述", type = OperationType)`
- 记录内容：操作用户、请求方法、参数、IP地址、执行耗时、成功/状态、错误信息
- 异步保存（@EnableAsync），不影响主业务流程

### 10.3 邮件服务

MailService支持真实SMTP和Mock两种实现：
- MockMailServiceImpl：开发环境使用，日志打印邮件内容
- MailServiceImpl：生产环境使用JavaMailSender发送
- 预留了到期提醒和逾期通知的邮件发送接入点

### 10.4 数据备份

System模块提供数据库备份功能，通过mysqldump命令实现SQL备份，支持：
- 手动触发备份
- 定时备份（cron配置）
- 备份文件列表查询
- 备份下载/删除
- 保留最近7天备份自动清理

### 10.5 数据库连接池

后端使用HikariCP连接池（Spring Boot默认），配置：
- 最小空闲连接：5
- 最大连接数：20
- 连接超时：30秒
- 最大生命周期：30分钟

---

## 附录：开发与部署

### 本地开发

1. **后端启动**：
   ```bash
   cd backend
   mvn spring-boot:run
   # 服务端口：9999
   # API前缀：http://localhost:9999/api
   # Swagger文档：http://localhost:9999/api/swagger-ui.html
   ```

2. **前端启动**：
   ```bash
   cd frontend
   npm install
   npm run dev
   # 开发服务器端口：Vite默认（通常5173）
   ```

### Docker部署

项目根目录提供docker-compose.yml，一键启动：
- MySQL：3306端口
- 后端：9999端口
- 前端：Nginx托管80端口，代理`/api`到后端

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 管理员 |
| user | 123456 | 普通用户 |
| test | 123456 | 普通用户 |
