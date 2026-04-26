# PRD：恋爱大师 RAG + MCP 能力升级

## 1. 背景

当前项目已经具备 Spring AI 智能体应用的基础能力，包括普通聊天、SSE 流式输出、RAG 知识库问答、Tool Calling、本地工具调用、图片搜索 MCP 子项目和 Vue 前端页面。

现阶段的问题是：这些能力已经存在，但产品定位和工程边界还不够清晰。RAG 能力主要内嵌在 `LoveApp` 中，MCP 能力主要体现为主应用调用外部 MCP 工具，尚未把“恋爱知识库问答能力”作为一个可被外部 AI 客户端复用的 MCP Server 能力输出。

本 PRD 的目标是基于现有项目，设计一个最小可落地的升级版本：把当前项目从“恋爱大师 AI 应用”升级为“领域知识库 RAG + Agent + MCP 平台”的可展示项目。

## 2. 假设

- 当前主项目目录为 `ai-agent-`。
- 后端技术栈继续使用 Java 21、Spring Boot、Spring AI、DashScope、PgVector 或 SimpleVectorStore。
- 前端继续使用已有 Vue 3 + Vite 项目，不重做整体 UI。
- 第一阶段优先服务学习、作品集展示和面试表达，不追求完整商业化后台。
- 第一阶段优先复用现有 API、配置、RAG 类和 MCP 子项目结构，避免大规模重构。

## 3. 产品目标

### 3.1 核心目标

将现有恋爱知识库 RAG 能力封装成可独立调用、可观测、可扩展的领域知识库服务，并通过 HTTP API 和 MCP Tool 两种方式对外提供能力。

### 3.2 可验证目标

- 用户可以通过现有前端或 HTTP API 发起恋爱知识库问答。
- 系统可以记录一次 RAG 查询的关键链路：原始问题、改写后问题、命中文档、最终回答。
- 外部 MCP Client 可以调用 `queryLoveKnowledge` 工具，获得基于恋爱知识库的回答。
- README 或文档中可以清楚说明项目同时具备 MCP Client 和 MCP Server 两类能力。
- 保留当前已有普通聊天、工具调用、图片搜索 MCP 能力，不破坏现有接口。

## 4. 非目标

第一阶段不做以下内容：

- 不做多租户、账号权限、付费系统。
- 不做复杂知识库后台管理系统。
- 不做完整 PDF、Word、网页等多格式摄取平台。
- 不强制引入 BM25、RRF、Rerank 等复杂检索链路。
- 不重写前端整体视觉风格。
- 不把所有 Agent 能力都改造成 MCP Server。

## 5. 用户与使用场景

### 5.1 学习者 / 项目作者

希望通过本项目理解 Spring AI、RAG、Tool Calling、MCP 的组合方式，并能把项目作为大模型应用工程实践作品。

### 5.2 面试官 / 评审者

希望看到项目不是简单调用大模型接口，而是具备清晰的知识库构建、检索增强、工具调用和协议集成能力。

### 5.3 外部 AI 客户端用户

希望在 Claude Desktop、Cursor、Copilot 等支持 MCP 的客户端中，直接调用恋爱知识库问答工具。

## 6. 一期功能范围

### 6.1 恋爱知识库服务封装

新增独立服务层，承接当前 `LoveApp.doChatWithRag` 中的核心 RAG 问答逻辑。

建议服务命名：

- `LoveKnowledgeService`

核心职责：

- 接收用户问题和会话 ID。
- 调用 `QueryRewriter` 进行查询改写。
- 调用现有向量库完成知识检索。
- 调用模型生成最终回答。
- 返回标准化问答结果。

建议返回结构：

```json
{
  "answer": "最终回答",
  "originalQuery": "用户原始问题",
  "rewrittenQuery": "改写后问题",
  "matchedDocuments": [
    {
      "filename": "恋爱常见问题和回答 - 单身篇.md",
      "status": "单身",
      "score": 0.82,
      "snippet": "命中的片段摘要"
    }
  ]
}
```

### 6.2 RAG 查询日志

新增轻量查询日志，第一阶段可以先用应用日志或内存结构，不强制上数据库。

需要记录：

- `chatId`
- `originalQuery`
- `rewrittenQuery`
- `matchedDocuments`
- `answer`
- `createdAt`
- `costMillis`

用途：

- 调试 RAG 命中效果。
- 面试演示检索链路。
- 后续扩展评估面板。

### 6.3 HTTP API

新增或补充知识库问答接口。

建议接口：

```text
GET /ai/love_app/rag/chat?message={message}&chatId={chatId}
```

返回建议：

- 简单版本返回 `String answer`。
- 增强版本返回结构化 JSON，包含回答和检索信息。

一期推荐使用结构化 JSON，因为它更适合展示 RAG 链路。

### 6.4 MCP Server Tool

新增一个面向外部 MCP Client 的工具：

```text
queryLoveKnowledge
```

工具描述：

```text
基于恋爱大师知识库回答单身、恋爱、已婚关系中的情感咨询问题。
```

入参：

- `query`：用户问题，必填。
- `chatId`：会话 ID，可选；如果未传则生成默认会话 ID。

出参：

- `answer`
- `rewrittenQuery`
- `matchedDocuments`

实现方式建议：

- 复用现有 `agent-image-search-mcp-server` 的子项目结构，新增独立 MCP Server 子项目；或
- 在当前主项目中新增 MCP Server profile。

一期推荐新增独立子项目，例如：

```text
agent-love-knowledge-mcp-server/
```

原因：

- 和现有图片搜索 MCP 子项目风格一致。
- 不影响主应用启动方式。
- 更容易在 README 中说明“外部 AI 客户端如何接入”。

### 6.5 前端展示入口

在已有前端中增加一个轻量入口，不重做页面。

建议：

- 在现有聊天页面增加“知识库问答”模式。
- 或新增一个简单页面 `KnowledgeRag.vue`。

页面展示：

- 用户问题输入框。
- AI 回答区域。
- 可折叠的“检索详情”，展示改写后问题和命中文档。

一期只需要满足演示，不做复杂知识库管理。

## 7. 二期功能候选

以下能力暂不进入一期，但可以作为 README 和面试中的后续规划：

- 文档上传和重新向量化。
- PgVector 持久化替代内存向量库。
- 多知识库 collection 管理。
- 检索评估面板。
- BM25 + 向量检索融合。
- Rerank 重排序。
- MCP Client 配置生成器。
- Docker Compose 一键启动 PostgreSQL + PgVector + 后端 + 前端。

## 8. 推荐技术方案

### 8.1 方案 A：最小增强主应用

在主项目中直接新增 RAG API 和 MCP Server 能力。

优点：

- 改动最少。
- 开发速度最快。

缺点：

- 主应用职责继续变重。
- MCP Server 启动方式和普通 Web 服务容易混在一起。

### 8.2 方案 B：主应用保留 API，新增知识库 MCP 子项目

主应用负责 HTTP API、前端和现有 Agent 能力；新增 `agent-love-knowledge-mcp-server` 子项目负责 MCP Tool 暴露。

优点：

- 边界清晰。
- 与现有 `agent-image-search-mcp-server` 结构一致。
- 更适合面试讲架构。

缺点：

- 需要维护一个新子模块。
- 需要处理知识库加载和配置复用。

### 8.3 方案 C：完整模块化 RAG 平台

引入多知识库、持久化、上传、评估、混合检索和可视化管理。

优点：

- 能力完整。
- 更接近生产级 RAG 平台。

缺点：

- 开发量明显变大。
- 容易偏离当前项目的学习和作品集目标。

### 8.4 推荐方案

一期采用方案 B。

理由：

- 能复用当前项目已有能力。
- MCP Server 的边界更清楚。
- 不会把主应用改得过重。
- 对面试表达的提升最大。

## 9. 数据流

### 9.1 HTTP RAG 问答

```text
用户 / 前端
  -> AiController
  -> LoveKnowledgeService
  -> QueryRewriter
  -> VectorStore 检索
  -> ChatClient 生成回答
  -> RAG 查询日志
  -> 返回 answer + 检索详情
```

### 9.2 MCP RAG 问答

```text
Claude / Cursor / Copilot
  -> MCP Client
  -> queryLoveKnowledge Tool
  -> LoveKnowledgeService 或等价 RAG 服务
  -> VectorStore 检索
  -> ChatClient 生成回答
  -> 返回结构化结果
```

## 10. 验收标准

### 10.1 功能验收

- 调用 HTTP RAG 接口时，可以得到基于恋爱知识库的回答。
- 返回结果包含原始问题、改写后问题和至少一个命中文档信息。
- MCP Client 能发现 `queryLoveKnowledge` 工具。
- MCP Client 调用 `queryLoveKnowledge` 后，可以得到知识库回答。
- 原有普通聊天和 Manus 智能体接口不受影响。

### 10.2 工程验收

- RAG 核心逻辑不继续堆在 `LoveApp` 中。
- 新增代码复用现有 `QueryRewriter`、`VectorStore`、`ChatClient` 能力。
- 新增类职责单一，命名清晰。
- 至少补充一个知识库服务测试或 MCP Tool 测试。

### 10.3 文档验收

- README 补充项目定位：Agent + RAG + MCP 平台。
- README 补充 MCP Client 和 MCP Server 的区别。
- README 给出 `queryLoveKnowledge` 的配置和调用示例。

## 11. 风险与约束

### 11.1 中文编码问题

当前部分 Markdown 和 Java 注释在终端中存在乱码显示。后续文档和代码修改需要统一使用 UTF-8。

### 11.2 向量库持久化

当前 `loveAppVectorStore` 使用 `SimpleVectorStore` 初始化，适合演示，但服务重启后依赖重新加载 classpath 文档。若后续需要生产化，应切换到 PgVector。

### 11.3 MCP 子项目复用

如果新增 `agent-love-knowledge-mcp-server` 子项目，需要决定是复制 RAG 逻辑，还是抽取公共模块复用。第一阶段建议少抽象，优先保证闭环跑通。

### 11.4 API Key 配置

DashScope、搜索 API、图片 API 等密钥必须继续放在本地配置或环境变量中，不提交真实密钥。

## 12. 里程碑

### M1：RAG 服务层整理

- 新增 `LoveKnowledgeService`。
- 将 RAG 问答逻辑从 `LoveApp` 中拆出。
- 新增结构化返回对象。

### M2：HTTP API 与日志

- 新增 RAG 问答 API。
- 记录查询日志。
- 补充基础测试。

### M3：MCP Server 工具

- 新增 `queryLoveKnowledge` MCP Tool。
- 补充 MCP Server 配置示例。
- 验证外部 MCP Client 可发现并调用工具。

### M4：前端演示与文档

- 前端增加知识库问答入口或模式。
- README 更新项目定位、启动方式和演示路径。
- 准备面试讲解版本。

## 13. 面试表达建议

可以将项目概括为：

> 基于 Spring AI 的垂直领域智能体平台，支持多轮对话、RAG 知识库问答、工具调用和 MCP 协议扩展。项目既能作为 MCP Client 调用外部工具，也能把自身领域知识库能力封装成 MCP Server，供 Claude、Cursor 等外部 AI 客户端调用。

重点亮点：

- 使用 Spring AI ChatClient 组织模型调用。
- 使用 Advisor 和 VectorStore 实现 RAG 检索增强。
- 使用 QueryRewriter 提升检索查询质量。
- 使用 Tool Calling 扩展 Web 搜索、文件操作、PDF 生成等能力。
- 使用 MCP 协议完成 AI 工具生态集成。
