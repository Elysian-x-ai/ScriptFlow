# ScriptFlow
一款AI辅助剧本创作工具，降低改编门槛，能够将3个章节以上的小说文本自动转换为结构化剧本，让作者可以快速获得可编辑、可进一步打磨的剧本初稿。

一、整体架构（微服务分层）

    客户端 → Next.js/Vue3前端 → API网关
            ↓
    Java业务集群(用户/项目/剧本/任务/存储) ⇄ 内部RPC/HTTP ⇄ Python AI编排服务
                                          ↓
                            多厂商大模型(DeepSeek/Claude/GPT4o/通义千问)

任务流转：小说上传→RabbitMQ 异步队列→AI 多 Agent 流水线结构化生成 YAML 剧本→结果落库→前端 SSE 实时推送生成进度

二、分层技术栈明细

1. 前端层（二选一，主推 Next.js，备选 Vue3）

表格

  模块     	技术选型                                    	选型说明                                    
  核心框架   	优先：Next.js14 (React18+App Router)备选：Vue3+Vite+TS	Next 支持 SSR/SSG，Vercel 一键部署演示；Vue 适配快速后台落地
  UI 组件  	ShadcnUI / Element Plus / Radix UI      	高颜值企业组件，适配剧本工作台布局                       
  剧本编辑器  	Monaco Editor (VSCode 内核)               	YAML 语法校验、自动补全、代码 Diff、自定义 Schema 校验    
  树形 / 拖拽	ReactFlow/VueFlow                       	剧本幕、场景拖拽排序，可视化大纲                        
  工程化    	TailwindCSS                             	样式开发提速，统一暗色 / 亮色主题                      

页面布局：左侧剧本大纲树｜中间 Monaco YAML 编辑区｜右侧 AI 对话助手｜底部剧本实时预览

2. Java 后端（业务核心，全量企业级底座）

- 基础环境：JDK21、SpringBoot3.5+、Spring Cloud Alibaba
- ORM：MyBatis-Plus
- 数据库：MySQL8.0（业务主数据：用户、项目、角色、任务、版本）+ MongoDB（海量剧本 YAML 草稿、历史版本）+ PostgreSQL (可选 pgvector，预留 RAG 知识库扩展)
- 缓存：Redis（Prompt 缓存、AI 返回结果缓存、接口限流）
- 消息队列：RabbitMQ（超长文本转换异步削峰）
- 对象存储：MinIO（本地部署）/ 阿里云 OSS（商用），存储小说源文件、导出 PDF/Word 剧本
- 权限框架：Sa-Token，SaaS 多租户、用户分级管控
- 接口文档：Knife4j
- AI 适配备选：SpringAI（简单模型调用可替代部分 Python 能力）
- 部署配套：Docker、K8s (生产)、Docker Compose (开发 / 演示)

负责范围：用户管理、项目管理、剧本版本管理、文件存储、任务状态管理、导出文件、计费、权限、回调 AI 结果、前端交互接口。

3. Python AI 微服务层（智能体编排核心）

- 运行环境：Python3.12、FastAPI（高性能接口，自动生成接口文档）
- AI 编排框架：LangGraph + LangChain（多 Agent 流水线）
- 数据校验：Pydantic（严格约束入参、模型出参 JSON Schema）
- 核心 Agent 流水线：原文分章→角色抽取→世界观提炼→剧情拆分→场景切割→对白生成→结构化 YAML 组装
- 模型适配：统一 AIProvider 适配器，无缝切换：DeepSeek、Claude3.5 Sonnet、GPT-4o、通义千问、文心一言

负责范围：长文本切片、多步骤 AI 拆解生成、结构化输出校验、Prompt 模板管理，独立弹性扩容不影响业务服务。

4. 大模型选型（分场景使用）

1. 正式生成剧本：Claude3.5 Sonnet（超长文本、结构化输出最优）、DeepSeek-V3（国产低成本首选）
2. 演示 / 低成本试用：GPT4o-mini、通义 Max
3. 私有化部署：Qwen 系列开源大模型

三、剧本统一 YAML Schema（全项目标准）

基于三份文档融合标准版，配套 JSON Schema 做 AI 输出强制校验，兼顾人类编辑、前端渲染、后续分镜 / TTS 扩展

yaml

    meta:
      title: "剧本名称"
      source_novel: "原著+章节范围"
      author: "作者/改编人"
      version: "v1.0"
      create_time: "时间戳"
    characters:
      - id: "char_xxx"
        name: "角色名"
        description: "人物外貌性格简介"
        voice_trait: "声线描述（预留TTS配音）"
    acts: # 影视标准分幕
      - act_id: "act_01"
        title: "第一幕标题"
        scenes:
          - scene_number: 1
            title: "单场标题"
            location: "内/外景+地点"
            time: "日/夜/黄昏"
            atmosphere: "环境氛围"
            present_char: ["char_xxx"]
            content:
              - type: action #动作描写
                text: "内容"
              - type: parenthetical #神态标注
                character_id: "char_xxx"
                text: "括号描述"
              - type: dialogue #人物对白
                character_id: "char_xxx"
                text: "台词内容"

四、企业级扩展配套能力

1. Prompt 工作室：可视化配置电影 / 短剧 / 动漫三类改编风格模版，自定义 Prompt 并持久化
2. 剧本版本管理：类 Git 版本回溯 + Monaco 文档 Diff 对比，留存每版修改记录
3. 多格式导出：Pandoc 实现 YAML 转标准 PDF、Word、FDX 影视专用剧本格式
4. 多人协同：Y.js+Monaco 实现在线协同编辑
5. 预留扩展位：分镜图生成（接入 AI 绘图）、角色配音（TTS）、AI 成片视频生成

五、运维 & 演示部署方案

1. 演示环境：前端 Vercel 免费托管，后端 4C8G 云服务器，Docker Compose 一键拉起全服务
2. 生产环境：Docker 打包 + K8s 容器编排，Prometheus+Grafana+ELK 做全链路监控日志
3. 访客试用：访客模式限制每日免费转换次数，降低试用成本

六、选型优势总结

1. 贴合 Java 开发背景：主体业务沿用熟悉技术栈，开发、维护成本最低
2. 最大化 Python AI 生态：LangGraph/LangChain 全原生支持，规避 Java 实现 AI 生态短板
3. 扩展性拉满：从网文→剧本→分镜→配音→AI 成片全链路预留字段与架构，无需重构
4. 落地快：Docker 一键部署，1 天完成演示环境上线，适合产品验证与商业化落地

