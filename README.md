<p align="center">
  <h1 align="center">ScriptFlow</h1>
  <p align="center">AI辅助剧本创作工具 —— 将小说自动转换为结构化剧本</p>
</p>

<p align="center">
  <a href="#特性">特性</a> •
  <a href="#架构概览">架构</a> •
  <a href="#技术栈">技术栈</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#项目结构">项目结构</a> •
  <a href="#API-概览">API</a> •
  <a href="#YAML-Schema">Schema</a> •
  <a href="#路线图">路线图</a>
</p>

---

## 演示视频地址
通过网盘分享的文件：七牛云-ai剧本-演示视频.mp4
链接: https://pan.baidu.com/s/1uBVy45Gq3PPqb9rqIFbBbg?pwd=zngw 提取码: zngw
## 特性

- **小说→剧本自动转换** — 上传小说文本，AI 多 Agent 流水线自动输出结构化剧本 YAML
- **增量生成** — 基于内容哈希检测章节变更，新增/修改章节仅处理差异部分，已有内容完整保留
- **7 阶段 AI Pipeline** — 章节切分 → 角色抽取 → 世界观提炼 → 剧情拆分 → 场景切割 → 对白生成 → YAML 组装
- **多模型支持** — 统一 Provider 适配器，无缝切换 DeepSeek、Claude、GPT-4o、通义千问
- **版本管理** — 类 Git 版本回溯，MinIO 存储历史 YAML，Monaco Diff 对比
- **异步任务** — RabbitMQ 消息队列解耦，SSE 实时推送生成进度
- **YAML 编辑器** — Monaco Editor 内置 YAML 语法高亮、校验、折叠
- **增量生成** — MD5 内容哈希比对，仅处理变更章节，大幅节省 token 和时间
- **企业级底座** — MyBatis-Plus ORM、Sa-Token 认证、Knife4j 接口文档

## 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    Next.js 前端                           │
│      Monaco Editor · 项目工作台 · 实时进度 · 版本管理       │
└──────────────┬──────────────────────────────────────────┘
               │ HTTP / SSE
┌──────────────▼──────────────────────────────────────────┐
│              Java 后端 (Spring Boot 3)                     │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐  │
│  │项目   │ │剧本   │ │任务   │ │存储   │ │导出   │ │系统   │  │
│  │模块   │ │模块   │ │模块   │ │模块   │ │模块   │ │模块   │  │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘  │
└──────────────┬──────────────────────────────────────────┘
               │ RabbitMQ / HTTP
┌──────────────▼──────────────────────────────────────────┐
│          Python AI 微服务 (FastAPI)                        │
│  ┌──────────────────────────────────────────────────┐    │
│  │        7 Agent Pipeline                          │    │
│  │  分章 → 角色 → 世界观 → 剧情 → 场景 → 对白 → YAML  │    │
│  └──────────────────────────────────────────────────┘    │
│  ┌────────────┐ ┌────────────┐ ┌──────────────────┐      │
│  │ AI Provider│ │    MQ     │ │   MinIO Client    │      │
│  │  适配器     │ │  消费者    │ │   对象存储         │      │
│  └────────────┘ └────────────┘ └──────────────────┘      │
└──────────────┬──────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────┐
│              基础设施                                      │
│    MySQL 8 · Redis 7 · RabbitMQ · MinIO                  │
└─────────────────────────────────────────────────────────┘
```

### 任务流转

```
小说上传 → Java 创建 Task → RabbitMQ 投递 → Python Worker 消费
  → AI Pipeline(7 Agents) → YAML 落 MinIO → RabbitMQ 回传结果
  → Java 更新状态 → SSE 推送前端
```

## 技术栈

### 后端 (Java)

| 模块 | 选型 | 说明 |
|------|------|------|
| 基础框架 | Spring Boot 3.2 + JDK 17 | 最新稳定版，虚拟线程就绪 |
| ORM | MyBatis-Plus 3.5 | 代码生成、分页、逻辑删除 |
| 数据库 | MySQL 8.0 | 业务主数据 |
| 缓存 | Redis 7 | Prompt 缓存、限流 |
| 消息队列 | RabbitMQ | 异步 AI 任务解耦 |
| 对象存储 | MinIO | 源文件 + YAML 剧本存储 |
| 权限 | Sa-Token | JWT 令牌、多租户 |
| 接口文档 | Knife4j / SpringDoc | OpenAPI 3.0 |

### AI 微服务 (Python)

| 模块 | 选型 | 说明 |
|------|------|------|
| 框架 | FastAPI | 高性能异步接口 |
| AI 适配 | OpenAI SDK (统一接口) | 适配 DeepSeek / Claude / GPT-4o 等 |
| Agent 架构 | 多 Agent 流水线 | 每阶段独立 Agent，JSON 中间结果传递 |
| 异步消费 | aio-pika | RabbitMQ 异步消费者 |
| 对象存储 | MinIO SDK | 读取小说内容、回写 YAML |

### 前端

| 模块 | 选型 |
|------|------|
| 框架 | Next.js 14 (App Router) |
| 语言 | TypeScript |
| 样式 | Tailwind CSS |
| 编辑器 | Monaco Editor |
| 状态管理 | Zustand |
| 组件库 | Radix UI + Lucide Icons |

## 快速开始

### 前置要求

- Docker & Docker Compose
- JDK 17+
- Node.js 18+
- Python 3.12+

### 1. 启动基础设施

```bash
docker compose -f docker/docker-compose.yml up -d
```

启动 MySQL、Redis、RabbitMQ、MinIO 四个服务。首次启动自动执行 `schema.sql` 建表。

### 2. 启动 Python AI 服务

```bash
cd scriptflow-ai-service
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt

# 配置 AI Provider (支持 deepseek / claude / openai / mock)
# 编辑 .env 文件:
#   ai_provider=deepseek
#   deepseek_api_key=sk-xxx

python -m app.main
```

服务默认监听 `0.0.0.0:8001`。

### 3. 启动 Java 后端

```bash
# Maven 编译
./mvnw clean install -DskipTests

# 启动 Boot 模块
cd scriptflow-boot
mvn spring-boot:run
```

服务默认监听 `8080` 端口，API 文档访问 `http://localhost:8080/swagger-ui.html`。

### 4. 启动前端

```bash
cd scriptflow-frontend
npm install
npm run dev
```

服务默认监听 `3000` 端口。

### 5. 访问

打开浏览器访问 `http://localhost:3000`，注册账号后即可创建项目并开始使用。

## 项目结构

```
scriptflow/
├── scriptflow-boot/              # Spring Boot 启动入口
├── scriptflow-common/            # 公共组件 (R 响应、异常、常量、工具类)
├── scriptflow-framework/        # 框架核心 (BaseService、全局异常处理、事件)
├── scriptflow-dal/              # 数据访问层 (实体、Mapper、SQL)
│   └── src/main/resources/sql/  # 数据库 Schema
├── scriptflow-module-system/    # 系统模块 (用户、角色、认证)
├── scriptflow-module-project/   # 项目模块 (项目、章节、角色、剧本)
├── scriptflow-module-task/      # 任务模块 (AI 任务提交、进度跟踪)
├── scriptflow-module-prompt/    # Prompt 模板管理
├── scriptflow-module-export/    # 导出模块 (PDF/Word)
├── scriptflow-storage/          # 存储模块 (MinIO 封装)
├── scriptflow-ai-client/        # AI 微服务 Feign 客户端
│
├── scriptflow-ai-service/       # Python AI 微服务
│   └── app/
│       ├── agents/              # 7 个 Agent 实现
│       │   ├── chapter_splitter.py    # ① 章节切分
│       │   ├── character_extractor.py # ② 角色抽取
│       │   ├── world_builder.py       # ③ 世界观提炼
│       │   ├── plot_splitter.py       # ④ 剧情拆分
│       │   ├── scene_cutter.py        # ⑤ 场景切割
│       │   ├── dialogue_generator.py  # ⑥ 对白生成
│       │   ├── yaml_assembler.py      # ⑦ YAML 组装
│       │   ├── character_merger.py    # 角色合并 (批量场景)
│       │   └── world_merger.py        # 世界观合并 (批量场景)
│       ├── providers/           # AI Provider 适配器
│       ├── mq/                  # RabbitMQ 消费者/发布者
│       ├── storage/             # MinIO 客户端
│       ├── pipeline.py          # Pipeline 编排
│       ├── worker.py            # 任务 Worker
│       ├── config.py            # 配置
│       └── main.py              # FastAPI 入口
│
├── scriptflow-frontend/         # Next.js 前端
│   └── src/
│       ├── app/                 # App Router 页面
│       │   ├── login/           # 登录
│       │   ├── register/        # 注册
│       │   └── (authenticated)/ # 认证后页面
│       │       ├── dashboard/   # 工作台
│       │       ├── projects/    # 项目列表 + 详情
│       │       │   └── [id]/    # 项目工作台 (剧本编辑)
│       │       └── admin/       # 管理后台
│       └── lib/
│           ├── api-client.ts    # API 客户端
│           └── stores/          # Zustand 状态管理
│
├── docker/                      # Docker Compose 配置
│   └── docker-compose.yml       # MySQL + Redis + RabbitMQ + MinIO
│
└── docs/
    ├── yaml-schema.md           # YAML Schema 定义与设计说明
    └── architecture.md          # 架构设计文档
```

## API 概览

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录获取 Token |
| POST | `/api/auth/register` | 注册 |
| GET | `/api/auth/userinfo` | 获取当前用户信息 |

### 项目

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/project/list` | 项目列表 |
| POST | `/api/project/create` | 创建项目 |
| GET | `/api/project/{id}` | 项目详情 |

### 剧本

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/project/script/project/{projectId}` | 获取项目剧本 |
| POST | `/api/project/script/generate/{projectId}` | 提交剧本生成任务 |
| GET | `/api/project/script/chapters/{projectId}` | 获取章节列表(含内容哈希) |
| GET | `/api/project/script/last-chapters/{projectId}` | 上次生成的章节号 |
| GET | `/api/project/script/last-chapter-hashes/{projectId}` | 上次生成的章节哈希 |
| POST | `/api/project/script/validate` | 验证 YAML 合法性 |
| GET | `/api/project/script/version/list/{scriptId}` | 版本列表 |
| POST | `/api/project/script/version/{scriptId}` | 创建新版本 |
| GET | `/api/project/script/yaml/list/{projectId}` | MinIO YAML 文件列表 |
| GET | `/api/project/script/yaml/content` | 读取 MinIO YAML 内容 |

### 任务

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/task/list` | 任务列表 |
| GET | `/api/task/{id}` | 任务详情 |
| GET | `/api/task/progress/{id}` | SSE 实时进度 |

完整 API 文档请参阅运行后 Knife4j 页面：`http://localhost:8080/swagger-ui.html`

## AI Pipeline

ScriptFlow 的核心是一个 7 阶段多 Agent 流水线，每阶段独立调用 LLM，前一阶段的 JSON 输出作为后一阶段的上下文输入。

```
输入: 小说章节 (JSON)
  │
  ▼
┌─────────────┐
│ ① 章节切分   │ 将原始文本按章节拆分为结构化 JSON 数组
│ Chapter     │ {chapterNo, title, content, wordCount}
│ Splitter    │
└──────┬──────┘
       ▼
┌─────────────┐
│ ② 角色抽取   │ 提取所有角色及其属性
│ Character   │ {name, alias, gender, personality, ...}
│ Extractor   │
└──────┬──────┘
       ▼
┌─────────────┐
│ ③ 世界观提炼  │ 提炼世界观设定 (时间、地点、势力、规则)
│ World       │
│ Builder     │
└──────┬──────┘
       ▼
┌─────────────┐
│ ④ 剧情拆分   │ 将小说按起因/发展/转折/高潮/结局拆分为情节单元
│ Plot        │
│ Splitter    │
└──────┬──────┘
       ▼
┌─────────────┐
│ ⑤ 场景切割   │ 为每个情节单元切割为场景，标注时空信息
│ Scene       │
│ Cutter      │
└──────┬──────┘
       ▼
┌─────────────┐
│ ⑥ 对白生成   │ 为每个场景生成动作描写/神态标注/台词
│ Dialogue    │
│ Generator   │
└──────┬──────┘
       ▼
┌─────────────┐
│ ⑦ YAML 组装  │ 将所有结构化数据组装为标准 YAML 剧本
│ YAML        │
│ Assembler   │
└──────┬──────┘
       ▼
输出: YAML 剧本 (符合标准 Schema)
```

对于超过 5 章的批量输入，Pipeline 自动启用 **批量模式**：

- 角色/世界观分批提取后通过 **CharacterMerger** / **WorldMerger** 合并
- 章节按 10 章一组分幕，逐幕生成剧情/场景/对白
- **增量模式** 下仅处理变更章节，未变章节从 `previousYaml` 直接保留

## YAML Schema

剧本输出遵循标准 Schema，详见 [docs/yaml-schema.md](docs/yaml-schema.md)。

核心结构：

```yaml
meta:
  title: "剧本名称"
  source_novel: "原著+章节范围"
  version: "v1.0"

characters:
  - id: "char_01"
    name: "角色名"
    description: "外貌性格简介"
    voice_trait: "声线描述（预留 TTS）"

acts:
  - act_id: "act_01"
    title: "第一幕"
    scenes:
      - scene_number: 1
        title: "场景标题"
        location: "内/外景+地点"
        time: "日/夜"
        atmosphere: "氛围"
        present_char: ["char_01"]
        content:
          - type: action
            text: "动作描写"
          - type: parenthetical
            character_id: "char_01"
            text: "低声地"
          - type: dialogue
            character_id: "char_01"
            text: "台词内容"
```

## 配置参考

### 环境变量 (Python AI 服务 `.env`)

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `ai_provider` | `deepseek` | AI 提供商: claude / deepseek / openai / mock |
| `deepseek_api_key` | — | DeepSeek API Key |
| `claude_api_key` | — | Claude API Key |
| `openai_api_key` | — | OpenAI API Key |
| `rabbitmq_host` | `localhost` | RabbitMQ 地址 |
| `minio_endpoint` | `localhost:9000` | MinIO 地址 |

### 应用配置 (Java `application.yml`)

| 配置项 | 说明 |
|--------|------|
| `scriptflow.jwt.secret` | JWT 签名密钥 (≥32 字符) |
| `scriptflow.ai.base-url` | AI 微服务地址 |
| `scriptflow.storage.endpoint` | MinIO 地址 |
| `scriptflow.rabbit.queue.task` | 任务队列名称 |

## 路线图

- [x] 小说章节管理 + 内容哈希变更检测
- [x] 7 Agent AI 流水线结构化生成
- [x] 增量生成（仅处理变更章节）
- [x] 版本管理（MinIO + 多版本表）
- [x] 多模型适配器（DeepSeek / Claude / GPT-4o）
- [x] YAML 在线编辑 + 语法校验
- [ ] 多格式导出（PDF / Word / FDX）
- [ ] 剧本大纲可视化拖拽（ReactFlow）
- [ ] 分镜图生成（AI 绘图接入）
- [ ] 角色 TTS 配音
- [ ] 在线协同编辑（Y.js）
- [ ] Prompt 工作室（可视化模板配置）

## 许可证

[MIT](LICENSE)
