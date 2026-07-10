# Stewie Blog · 后端

Stewie Blog 的 Spring Boot 后端，提供文章、分类、标签、作者、评论、点赞与 JWT 鉴权接口，并内置文章封面上传。配套前端见 [`../Stewie-blog/README.md`](../Stewie-blog/README.md)。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.16 | 应用框架 |
| Java | 17 | 运行环境 |
| MyBatis-Plus | 3.5.12 | ORM（`+mybatis-plus-jsqlparser` 分页/乐观锁拦截器） |
| Spring Security | 3.5.x | 安全框架（无状态 + JWT 过滤器） |
| jjwt | 0.12.6 | JWT 签发 / 校验（HS256） |
| Knife4j | 4.5.0 | OpenAPI 3 文档（Spring Boot 3 / Jakarta） |
| Flyway | 随 Boot | 数据库版本迁移 |
| Redis | — | 浏览量 / 点赞去重缓存 |
| MySQL | 8.0 | 关系型数据库 |
| Lombok | — | 简化实体代码 |

## 项目结构

```
src/main/java/com/stewie/blog/
├── common/          # 统一响应 / 业务异常 / 全局异常处理
├── config/          # SecurityConfig / WebConfig(静态资源) / Swagger/Knife4j 等
├── controller/
│   ├── pub/         # 公开接口（文章 / 互动 / 分类标签作者）
│   ├── auth/        # 登录 / 当前用户
│   └── admin/       # 管理接口（文章 CRUD / 封面上传 / 评论审核）
├── dto/             # request / response / vo
├── entity/          # 数据库实体（Post / User / Comment / LikeRecord ...）
├── mapper/          # MyBatis-Plus Mapper
├── security/        # JwtUtil / LoginUser / UserDetailsServiceImpl / JwtAuthenticationFilter / 处理器
├── service/         # 业务接口 + impl
└── util/            # 客户端指纹等工具
src/main/resources/
├── application.yml        # 默认配置（server.port=8081）
├── application-dev.yml    # 开发环境（云数据库 CDB / 本地 Redis）
├── application-prod.yml   # 生产环境
└── db/migration/          # Flyway（V1–V4，baseline=2）
```

## 快速开始

### 环境注意（重要）

本机系统 `JAVA_HOME` 指向 **Java 8**（其它公司项目需要），而本项目需要 **Java 17**。项目内置 `build.cmd` 会在**不修改系统环境变量**的前提下临时切换为 JDK 17：

```bat
:: 在 stewie-blog-spring 目录下
build.cmd package          :: 等价于 mvnw package（强制 JDK 17）
build.cmd compile          :: 仅编译
```

> IDEA 用户：Project SDK 与 Maven Runner JRE 均设为 17 即可，无需改动系统 `JAVA_HOME`。

### 启动

```bat
:: 1) 打包（JDK 17）
build.cmd package -DskipTests

:: 2) 运行（命令行参数端口优先级最高，规避 spring-boot:run 的 fork 随机端口问题）
java -jar target/stewie-blog-spring-0.0.1-SNAPSHOT.jar --server.port=8081
```

启动后接口根路径 `http://localhost:8081`，API 文档（Knife4j）：**http://localhost:8081/doc.html**。

### 数据库与 Flyway

- 使用 MySQL 8，连接信息在 `application-dev.yml`（云数据库 CDB，密码含 `!` 须用单引号）。
- Flyway `baseline-version=2`：因 `t_post` 等表与种子数据已手工执行 `V1__init_schema.sql` / `V2__seed_data.sql`，故 V1/V2 跳过，迁移从 **V3** 起（`V3__seed_comments.sql`、`V4__add_cover.sql`）。
- 首次启动会自动执行未应用的迁移。

### 鉴权密钥

`application.yml` 中：

```yaml
jwt:
  secret: ${JWT_SECRET:默认占位密钥}
  expiration: 86400000   # 24h
```

**生产环境必须通过环境变量覆盖**：`set JWT_SECRET=一段至少 256bit 的随机串` 再启动，否则使用默认占位密钥存在安全风险。

### 文件上传（封面图）

- `POST /api/admin/upload` 接收图片（校验 `image/*`、≤5MB、扩展名白名单），写入 `upload.path`（默认 `./uploads`，可用环境变量 `UPLOAD_DIR` 指向持久化目录），返回 `/uploads/<uuid>.<ext>`。
- 该路径在 `WebConfig` 中映射为静态资源，`SecurityConfig` 已放行 `GET /uploads/**`，前端经 Vite 代理访问。

## API 概览

所有接口前缀为 `/api`，基础 URL `http://localhost:8081`。

### 公开接口（无需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/posts` | 文章分页列表（支持分类 / 标签 / 关键词筛选，返回封面与标签） |
| GET | `/posts/popular` | 热门文章 |
| GET | `/posts/{slug}` | 文章详情 |
| GET | `/categories` | 分类列表 |
| GET | `/tags` | 标签列表 |
| GET | `/author` | 作者信息 |
| GET | `/posts/{id}/like` | 点赞状态（按客户端指纹） |
| POST | `/posts/{id}/like` | 点赞 / 取消（幂等） |
| GET | `/posts/{id}/comments` | 评论树（仅已通过） |
| POST | `/posts/{id}/comments` | 提交评论（进入待审核） |

### 鉴权接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录，返回 JWT（默认账号 `admin / admin123`） |
| GET | `/auth/me` | 当前登录用户（需 `Authorization: Bearer <token>`） |

### 管理接口（`/api/admin/**`，均需 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/upload` | 封面图上传，返回 URL |
| GET | `/admin/posts` | 管理端文章列表（含草稿，支持状态筛选 + 分页） |
| GET | `/admin/posts/{id}` | 文章编辑回显 |
| POST | `/admin/posts` | 新建文章（含封面上传 URL + 多标签自动建联） |
| PUT | `/admin/posts/{id}` | 更新文章 |
| DELETE | `/admin/posts/{id}` | 删除文章（逻辑删除） |
| GET | `/admin/comments` | 评论列表（按状态筛选） |
| PUT | `/admin/comments/{id}` | 审核（通过 / 标记垃圾） |
| DELETE | `/admin/comments/{id}` | 删除评论 |

### 跨域（CORS）

`SecurityConfig` 已放行 `http://localhost:5173` 与 `http://localhost:4173`；其它来源请在后端配置或经反向代理同源部署。

## 说明

- 当前仅 `admin` 单用户（种子数据 `admin / admin123`，BCrypt 存储）。
- 未登录用户访问 `/api/admin/**` 返回 `401`；公开内容接口对读者完全开放。
