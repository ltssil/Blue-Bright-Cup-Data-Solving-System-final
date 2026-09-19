# 🏆 Academic Competition Management System

一个面向高校学科竞赛场景的**学科竞赛管理系统**，采用前后端分离架构，实现竞赛信息、学生、教师、团队、报名、获奖等业务数据的管理与展示。

项目后端基于 Spring Boot 构建，前端使用 Vue 3 + Vite，通过 REST API 完成前后端数据交互；后端同时集成 MySQL、JDBC 与 Apache POI，用于业务数据持久化和 Excel 数据处理。

## ✨ 项目特点

- 🎓 **竞赛业务管理**：围绕高校学科竞赛场景组织系统功能。
- 🖥️ **前后端分离**：Spring Boot 后端 + Vue 3 前端。
- 🗄️ **MySQL 数据库**：通过 Spring JDBC 访问关系型数据库。
- 📊 **Excel 数据处理**：使用 Apache POI 处理 Excel 文件。
- 🌐 **RESTful API**：后端使用 Spring Web 提供 HTTP 接口。
- 🧪 **测试支持**：使用 Spring Boot Test / JUnit Platform 进行测试。
- ⚙️ **Gradle 构建**：项目使用 Gradle 管理依赖和构建流程。

## 🏗️ 系统架构

```text
┌──────────────────────┐
│      Vue 3 前端       │
│  Vue Router + Axios   │
└──────────┬───────────┘
           │ HTTP / REST API
           ▼
┌──────────────────────┐
│   Spring Boot 后端    │
│   Spring Web / JDBC   │
└──────────┬───────────┘
           │
     ┌─────┴─────┐
     ▼           ▼
  MySQL       Excel
  数据库      Apache POI
```

## 🧰 技术栈

| 技术 | 用途 |
|---|---|
| Java 21 | 后端开发语言 |
| Spring Boot 3.2.1 | 后端应用框架 |
| Spring Web | REST API / Web 接口 |
| Spring JDBC | 数据库访问 |
| MySQL | 数据持久化 |
| Apache POI 5.2.5 | Excel 文件处理 |
| Lombok | Java 开发辅助 |
| Vue 3 | 前端框架 |
| Vue Router | 前端路由 |
| Axios | HTTP 请求 |
| Vite | 前端构建工具 |
| Gradle | 项目构建与依赖管理 |

后端使用 Java 21，并引入 Spring Boot 3.2.1、Spring Web、Spring JDBC、MySQL Connector/J、Apache POI 和 Lombok 等依赖；前端使用 Vue 3、Vue Router、Axios 和 Vite。

## 📁 项目结构

```text
Blue-Bright-Cup-Data-Solving-System-final/
├── academic-front/          # Vue 3 前端项目
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── vite.config.*
│
├── src/
│   ├── main/
│   │   ├── java/             # Spring Boot 后端 Java 源码
│   │   └── resources/        # 后端配置及资源文件
│   └── ...
│
├── tests/                    # 测试代码
├── build.gradle              # Gradle 构建配置
├── settings.gradle           # Gradle 项目配置
├── gradlew                   # Gradle Wrapper
├── gradlew.bat               # Windows Gradle Wrapper
└── README.md
```

项目根模块名称为 `academicCompetition`。

## 🚀 快速开始

### 1. 环境要求

建议准备：

- JDK 21
- Gradle Wrapper（项目已提供）
- MySQL
- Node.js 20.19+ 或 22.12+
- npm

### 2. 启动后端

Windows：

```bash
./gradlew.bat bootRun
```

Linux / macOS：

```bash
./gradlew bootRun
```

也可以在 IntelliJ IDEA 中直接运行 Spring Boot 启动类。

### 3. 配置 MySQL

后端使用 Spring JDBC + MySQL Connector/J，需要根据本地数据库环境配置数据库连接信息。

请检查：

- MySQL 服务是否启动
- 数据库名称是否正确
- 用户名和密码是否正确
- 后端 JDBC 配置是否与本地环境一致

> 不要将生产环境数据库密码等敏感信息提交到 GitHub。

### 4. 启动前端

进入前端目录：

```bash
cd academic-front
```

安装依赖：

```bash
npm install
```

启动开发服务器：

```bash
npm run dev
```

构建生产版本：

```bash
npm run build
```

预览构建结果：

```bash
npm run preview
```

## 📊 数据处理

项目后端集成 Apache POI，用于 Excel 文件的读取、解析和处理，可用于竞赛相关数据的批量导入、导出等场景。

核心依赖包括：

```text
Apache POI
├── poi
└── poi-ooxml
```

## 🧪 测试

后端使用 Spring Boot Test，并通过 JUnit Platform 执行测试：

```bash
./gradlew test
```

Windows：

```bash
./gradlew.bat test
```

## 🔧 开发说明

项目采用前后端分离方式组织代码：

- `academic-front` 负责页面、路由以及前端交互。
- `src/main/java` 负责 Spring Boot 后端业务逻辑和 REST API。
- `src/main/resources` 存放后端配置及资源。
- MySQL 负责业务数据持久化。
- Apache POI 负责 Excel 数据处理。

## 📌 项目定位

本项目主要用于高校学科竞赛管理场景的系统设计与开发实践，重点覆盖：

- Web 全栈项目开发
- Spring Boot 后端开发
- Vue 3 前端开发
- RESTful API 设计
- MySQL 数据库应用
- Excel 数据处理
- 前后端分离架构

## 📄 License

本项目主要用于学习、课程设计及竞赛项目实践。