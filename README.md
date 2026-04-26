# AI Agent 项目

基于 Spring AI 的智能体示例项目，包含后端服务、前端页面、工具调用链路，以及一个图片搜索 MCP 子项目。
<img width="1511" height="762" alt="image" src="https://github.com/user-attachments/assets/1e3599cb-9a3d-4d7c-9201-240a4f3312b9" />

这个仓库适合作为 Spring AI Agent 的实战起点，覆盖了以下能力：
- 普通聊天与流式输出
- ReAct 风格超级智能体
- Tool Calling 工具调用
- RAG / 向量检索相关集成
- PDF 生成
- 前后端联调
- MCP 服务扩展

## 项目结构

```text
.
├─ src/                         后端 Spring Boot 主项目
├─ agent-frontend/        前端 Vue 3 + Vite 项目
├─ agent-image-search-mcp-server/  图片搜索 MCP 子项目
├─ application-local.yml        本地开发配置（已被忽略，不会提交）
└─ pom.xml                      后端 Maven 配置
```

## 技术栈

- Java 21
- Spring Boot 3.4
- Spring AI 1.0
- Spring AI Alibaba DashScope
- Vue 3
- Vite
- iText PDF
- Jsoup
- PostgreSQL / PGVector（按需启用）
- MCP Client / MCP Server

## 主要能力


### 1. AI 聊天应用

后端提供基础聊天与流式响应接口，可接入 DashScope 模型完成普通问答场景。
<img width="978" height="673" alt="image" src="https://github.com/user-attachments/assets/6266bb17-870e-4e22-9256-62b526eef11a" />

### 2. 超级智能体

项目内置 `AgentManus` 风格 Agent，支持根据用户目标自动分步执行任务，例如：
- 搜索武汉热门景点
- 规划一日游路线
- 调用 PDF 工具生成文档
<img width="1070" height="856" alt="image" src="https://github.com/user-attachments/assets/14daab61-aaed-4d8a-8f61-069e38c2121e" />

### 3. 工具调用

当前仓库已经集成并验证过以下工具链：
- `searchWeb`
- `scrapeWebPage`
- `writeFile`
- `readFile`
- `downloadResource`
- `executeTerminalCommand`
- `generatePDF`
- `doTerminate`

### 4. PDF 生成

已修复中文 PDF 空白问题，当前会优先使用 Windows 本机中文字体并嵌入到 PDF 中。

### 5. MCP 子项目

仓库包含 `agent-image-search-mcp-server`，可作为后续接入图片搜索能力的扩展入口。

## 本地开发环境

建议环境：
- JDK 21
- Maven Wrapper（仓库已自带）
- Node.js 18+
- npm

## 配置说明

项目默认启用 `local` profile：

```yml
spring:
  profiles:
    active: local
```

因此本地开发建议在仓库根目录维护 `application-local.yml`。

至少需要配置：

```yml
spring:
  ai:
    dashscope:
      api-key: 你的 DashScope Key

search-api:
  api-key: 你的 SearchAPI Key
```

说明：
- `spring.ai.dashscope.api-key` 必填，否则后端启动时模型或向量相关初始化会失败
- `search-api.api-key` 主要用于联网搜索工具
- 如果你要启用图片 MCP 或外部资源能力，可能还需要补充对应第三方 API Key

## 启动方式

### 1. 启动后端

在仓库根目录执行：

```powershell
.\mvnw.cmd spring-boot:run
```

默认地址：
- 接口基址：`http://localhost:8123/api`
- 健康检查：`http://localhost:8123/api/health`
- Swagger：`http://localhost:8123/api/swagger-ui.html`

### 2. 启动前端

```powershell
cd .\agent-frontend
npm install
npm run dev
```

默认前端端口是 `3000`。如果端口被占用，Vite 会自动切到其他端口。

### 3. 构建前端

```powershell
cd .\agent-frontend
npm run build
```

## 已验证的关键链路

当前仓库已经完成并验证了以下修复：

- 修复超级智能体直答场景下的异常循环
- 修复工具调用注册方式错误导致的智能体对话失败
- 修复长工具参数回灌造成的 `function.arguments` 非法 JSON 报错
- 修复中文 PDF 生成为空白的问题
- 修复智能体执行中消息历史不可变导致的步骤失败问题

已验证通过的场景示例：

```text
请帮我查找武汉市的人气景点，规划一日游并生成pdf
```

## 测试

可运行的核心回归测试：

```powershell
.\mvnw.cmd -q test "-Dtest=ToolCallAgentTest,PDFGenerationToolTest"
```

## 常见问题

### 1. 后端启动失败

优先检查：
- `application-local.yml` 是否存在
- DashScope Key 是否可用
- 本机 Java 是否为 21

### 2. 超级智能体回复慢

这类任务会经过多轮工具调用，属于正常现象；如果搜索或模型接口较慢，整体耗时会明显增加。

### 3. 前端端口变化

如果默认端口被占用，Vite 会自动切换端口，实际以终端输出为准。

## 说明

本仓库适合作为学习和二次开发的基础版本。如果你要继续做产品化，建议下一步补充：
- 更清晰的前端下载入口
- 环境变量与配置模板
- Docker / Compose 部署说明
- 统一的 MCP 配置方式
- 更完整的集成测试
