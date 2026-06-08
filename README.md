# 图书管理系统

一个现代化的图书管理系统，采用前后端分离架构（Vue 3 + Spring Boot 3.x + MySQL 8.0），提供完整的图书管理、借阅管理、用户权限管理和系统管理功能。

## How to Run

### 环境要求

- Docker & Docker Compose

### 启动项目

```bash
docker-compose up --build -d
```

启动完成后，所有服务（MySQL、后端、前端）会自动构建并运行。

### 停止项目

```bash
docker-compose down
```

### 查看日志

```bash
docker-compose logs -f
```

## Services

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 8081 | http://localhost:8081 |
| 后端 API | 9999 | http://localhost:9999/api |
| API 文档 | 9999 | http://localhost:9999/api/swagger-ui.html |
| MySQL | 3307 | MySQL 8.0 数据库 |

## 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 普通用户 | user | 123456 |
| 测试用户 | test | 123456 |

## 短信验证码说明

登录和注册流程均需要短信验证码验证。当前为开发环境，**短信验证码不会实际发送短信**，而是通过页面弹窗（ElMessage）直接展示验证码，方便测试使用。

- **登录页面**：输入用户名后点击"获取验证码"，系统根据用户名查找账号绑定的手机号，验证码通过弹窗显示
- **注册页面**：输入手机号后点击"获取验证码"，验证码通过弹窗显示

> 生产环境需接入短信服务商（如阿里云短信、腾讯云短信）替换当前的模拟发送逻辑。

## 题目内容

设计并开发一个完整的图书管理系统，采用前后端分离架构。前端使用Vue框架构建用户界面，后端采用Java 17语言、Spring Boot 3.x版本开发RESTful API，数据库使用MySQL存储系统数据。系统需实现图书管理功能（包括图书信息的添加、查询、修改、删除）和用户借书功能（包括借书、还书、续借、查询借阅记录）。技术要求：- 后端禁止使用JPA或MyBatis-Plus，需采用原生MyBatis进行数据库操作，需实现自定义的BaseMapper和通用Service层封装- 前端需使用Vue 3及以上版本，配合Vue Router 4+实现路由管理，使用Pinia进行状态管理，采用Element Plus或Ant Design Vue组件库构建UI界面- 数据库设计需包含至少以下表：用户表(users)、图书表(books)、借阅记录表(borrow_records)、图书分类表(categories)，需设计合理的表关系、字段类型及约束- 实现RESTful API设计规范，包含适当的请求验证、错误处理和统一响应格式，使用Swagger/OpenAPI生成API文档- 前后端通信采用JWT进行身份认证，实现基于角色的访问控制(RBAC)权限管理系统项目组织：- 在当前工作区创建新的项目文件夹，使用英文命名（如"library-management-system"）- 采用行业标准的项目结构，前后端代码需分离存放（使用frontend和backend子目录）- 提供完整的源代码实现，包括所有功能模块、配置文件和依赖管理文件（pom.xml/package.json）- 实现前后端统一的代码规范和提交规范，使用ESLint/Prettier进行代码格式化功能要求：1. 图书管理功能：- 图书信息的录入、编辑、查询和删除，支持批量导入导出- 图书分类管理（添加、编辑、删除分类）- 图书库存管理（入库、出库、库存预警）- 图书搜索功能（支持按书名、作者、ISBN等多条件搜索，实现模糊查询和高级筛选）- 图书封面上传与预览功能2. 用户借书功能：- 用户注册与登录（包含验证码、密码加密存储）- 图书借阅操作（包含借阅期限设置，默认为30天）- 图书归还操作（支持逾期罚款计算）- 图书续借功能（最多续借1次，续期30天）- 借阅历史记录查询（支持按时间、状态筛选）- 借阅到期提醒（提前3天提醒，支持系统通知和邮件通知）3. 权限管理功能：- 用户角色管理（管理员、普通用户）- 权限分配与控制（不同角色可见菜单和操作权限不同）- 用户信息管理（查看、编辑、禁用用户账号）4. 系统管理功能：- 系统日志记录（用户操作日志、登录日志）- 数据备份与恢复功能- 系统参数配置（如默认借阅天数、续借次数限制等）交付标准：- 提供完整可运行的前后端源代码，包含详细的代码注释和Javadoc/TSDoc文档- 确保所有功能模块完整可用，业务逻辑清晰合理，无明显bug- 前端界面需采用现代化设计风格，实现响应式布局，支持PC端和移动端访问- 提供数据库脚本（包含初始化数据）和详细的部署说明文档- 实现基本的安全机制（如用户认证、权限控制、防SQL注入、XSS防护）- 提供单元测试和集成测试代码，核心功能测试覆盖率不低于70%- 系统性能要求：页面加载时间<2秒，API响应时间<500ms

---

## 技术栈

- **前端**：Vue 3 + TypeScript + Vite 5 + Element Plus + Pinia + Vue Router 4 + Axios
- **后端**：Java 17 + Spring Boot 3.x + MyBatis + Spring Security + JWT + Swagger/OpenAPI 3.0
- **数据库**：MySQL 8.0
- **部署**：Docker + Docker Compose + Nginx

## 项目结构

```
├── backend/                     # 后端 (Spring Boot)
│   ├── src/main/java/com/library/
│   │   ├── common/              # 公共模块（响应、异常、工具）
│   │   ├── security/            # 安全模块（JWT、权限）
│   │   ├── module/              # 业务模块
│   │   │   ├── auth/            # 认证
│   │   │   ├── book/            # 图书
│   │   │   ├── borrow/          # 借阅
│   │   │   ├── category/        # 分类
│   │   │   ├── notification/    # 通知
│   │   │   ├── user/            # 用户
│   │   │   ├── role/            # 角色
│   │   │   └── system/          # 系统
│   │   └── aspect/              # AOP 切面
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── mapper/              # MyBatis XML
│   │   └── db/                  # SQL 脚本
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/                    # 前端 (Vue 3)
│   ├── src/
│   │   ├── api/                 # API 接口
│   │   ├── layouts/             # 布局
│   │   ├── router/              # 路由
│   │   ├── stores/              # Pinia 状态
│   │   ├── utils/               # 工具函数
│   │   └── views/               # 页面
│   ├── package.json
│   ├── nginx.conf
│   └── Dockerfile
│
├── docker-compose.yml
├── .gitignore
└── README.md
```
