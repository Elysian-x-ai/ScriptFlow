# 剧本 YAML Schema 定义与设计说明

## 概述

ScriptFlow 的核心输出是**结构化剧本 YAML**，它位于整个 AI pipeline 的末端，是所有上游 Agent（角色抽取、世界观提炼、剧情拆分、场景切割、对白生成）的最终汇聚产物。该 YAML 剧本同时也是人类编辑的起点——作者在 Monaco Editor 中直接编辑这份 YAML，后续的版本管理、Diff 对比、多格式导出均依赖其结构。

因此，Schema 的设计目标可概括为：

1. **AI 友好**：结构化程度足以让 LLM 稳定生成，字段边界清晰，降低幻觉
2. **人类可读**：YAML 格式本身即具备良好的可读性，字段命名贴近影视行业术语
3. **可扩展**：预留下游消费字段（TTS、分镜），不因当前阶段未使用而删减
4. **增量兼容**：支持多个 YAML 片段合并（增量生成场景），不依赖全局唯一序号

---

## 完整 Schema

```yaml
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
    voice_trait: "声线描述（预留 TTS 配音）"

acts:
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
          - type: action
            text: "动作描写"
          - type: parenthetical
            character_id: "char_xxx"
            text: "括号神态/动作标注"
          - type: dialogue
            character_id: "char_xxx"
            text: "台词内容"
```

---

## 三层结构设计

### meta 层（剧本元信息）

| 字段 | 类型 | 说明 | 设计原因 |
|------|------|------|----------|
| `title` | string | 剧本标题 | 标识剧本身份，在多版本管理中作为文件标题展示 |
| `source_novel` | string | 原著 + 章节范围 | 追溯来源，增量生成时标明本次覆盖的章节区间，辅助编辑判断完整性 |
| `author` | string | 作者/改编人 | 协作场景下标识责任人 |
| `version` | string | 版本号 | 与 `pro_script_version` 表联动，支持版本回溯 |
| `create_time` | timestamp | 生成时间 | 排序和时间线回溯 |

**设计决策**：meta 层保持最小化。不存放角色数、章节数等派生数据（可从其他层推导），避免写入时产生不一致。

### characters 层（角色库）

角色定义从剧本正文中抽离为独立列表，以 `id` 作为唯一标识符在全篇中引用。

| 字段 | 类型 | 说明 | 设计原因 |
|------|------|------|----------|
| `id` | string (`char_xxx`) | 角色唯一标识 | 跨幕、跨场景引用的锚点。前缀 `char_` 便于全文搜索和正则提取 |
| `name` | string | 角色名 | 显示名，可重复（允许多个同名的龙套角色） |
| `description` | text | 外貌+性格 | 供 AI 生成对白时保持角色一致性，也供人类编辑快速回顾角色设定 |
| `voice_trait` | string | 声线描述 | **预留字段**。当前 pipeline 不使用，但保留以支持未来的 TTS 配音集成。如果在 schema 成型后再添加，需要改动所有历史版本的 YAML parser，成本更高 |

**设计决策**：角色列表放在 `acts` 之前（而非内嵌在每个场景中），原因：
- AI pipeline 在早期阶段（Agent 2）已完成角色抽取，此时还不知晓具体的幕/场景划分
- 人类编辑需要全局角色一览，而不是在场景中逐个查找
- 增量生成时，新增场景只需引用已有角色 ID，无需重复定义

### acts → scenes → content 三层嵌套

#### acts（幕）

| 字段 | 类型 | 说明 |
|------|------|------|
| `act_id` | string (`act_xx`) | 幕的唯一标识 |
| `title` | string | 幕标题 |
| `scenes` | array | 该幕包含的场景列表 |

**为什么分幕？**
- **影视工业标准**：剧本天然以「幕」为高级结构单元，每幕对应故事的一个重大转折
- **AI 分批处理**：pipeline 按每 10 章一组分组为幕（`_group_into_acts`），单幕的 token 量在模型上下文窗口内可控
- **增量友好**：新增章节通常形成新幕或追加到最后一幕，不需要重排已有场景的 `act_id`

#### scenes（场景）

场景是剧本的最小**时空单元**，一个场景 = 同一时空下一段连续剧情。

| 字段 | 类型 | 说明 | 设计原因 |
|------|------|------|----------|
| `scene_number` | int | 场景序号（幕内唯一） | 整数序号比 UUID 更易读，人类编辑可以在讨论中说"第三幕第 5 场" |
| `title` | string | 场景标题 | 概括场景内容，方便浏览大纲 |
| `location` | string | 地点（内/外景+地点） | 影视拍摄中的标准场地标注，如"内景-林家大院正厅" |
| `time` | string | 时间（日/夜/黄昏） | 决定灯光、摄影风格，标准电影剧本格式的必需元素 |
| `atmosphere` | string | 环境氛围 | 指导 AI 生成场景描述时的语气和用词，如"阴森压抑" |
| `present_char` | array[string] | 本场出场角色 ID 列表 | 用于场景规划（Agent 5 阶段即确定谁出场），后续对白生成阶段以此约束"不要生成未出场角色的台词" |
| `content` | array | 具体内容序列（见下） | — |

`present_char` 的设计意图常被误解为冗余（因为 content 中 dialogue 也已标注 `character_id`），但实际上两者服务于不同阶段：
- `present_char` 在**场景切割阶段**（Agent 5）确定，是规划性信息
- `content` 中的 `character_id` 在**对白生成阶段**（Agent 6）确定，是执行性信息
- 两者可交叉校验：如果 dialogue 中的角色不在 `present_char` 中，说明 AI 产生了幻觉

#### content（内容序列）

content 是一个有序数组，每个元素包含 `type` 判别器。这是 Schema 中唯一使用**带判别器的联合类型**的地方。

```yaml
content:
  - type: action
    text: "林教授站起身，走到窗边。"
  - type: parenthetical
    character_id: "char_01"
    text: "低声地"
  - type: dialogue
    character_id: "char_01"
    text: "这件事比你们想象的要复杂得多。"
```

**三种类型**：

| type | 含义 | 附加字段 | 说明 |
|------|------|----------|------|
| `action` | 动作/环境描写 | — | 不绑定特定角色，纯粹叙述性文字 |
| `parenthetical` | 神态/动作标注 | `character_id` | 紧跟在 dialogue 之前，标注说话人的语气或动作。源自电影剧本格式中的括号注释 |
| `dialogue` | 人物对白 | `character_id` | 角色台词。`text` 即说话内容 |

**为什么用 type 判别器而不是分开的字段？**
- content 是一个**有序序列**，action → parenthetical → dialogue 的顺序构成完整的叙事流
- 如果用三个独立字段（`actions: [...]`, `dialogues: [...]`），顺序信息会丢失，除非给每个条目再加序号
- type 判别器模式是 YAML/JSON schema 中表达联合类型的标准方式，前端渲染时可用 `switch(type)` 分发到不同 UI 组件

---

## 为什么选择 YAML 而非 JSON 或 XML

| 维度 | YAML | JSON | XML |
|------|------|------|-----|
| **人类可读性** | 最佳，缩进表达层级 | 一般，大量花括号 | 差，尖括号噪音多 |
| **注释支持** | `#` 注释 | 不支持 | 支持 `<!-- -->` |
| **AI 生成稳定性** | 较好，缩进错误即可检测 | 极好，括号匹配严格 | 中等，标签闭合易错 |
| **Git Diff 友好度** | 高，行级 diff 清晰 | 中，长行 diff 模糊 | 低，标签膨胀 |
| **前端解析** | 需 js-yaml 库 | 原生 JSON.parse | 需 DOMParser |
| **Schema 校验** | 需第三方（如 KCL） | JSON Schema 标准 | XSD 标准 |

**核心决策理由**：
1. **人类编辑场景**：作者在 Monaco Editor 中直接编辑剧本，YAML 的缩进结构天然可读，而 JSON 的花括号嵌套会降低编辑体验
2. **注释能力**：AI 生成的 YAML 中可嵌入 `#` 注释来解释生成决策（如 `# 此处原书有删节`），这些注释在编辑时提供上下文，在解析时被忽略
3. **版本管理**：剧本按版本存储在 MinIO 和 `pro_script_version` 表中，行级 diff 对 YAML 友好——新增一个场景就是几行新增，而 JSON 的一个场景变更可能导致整个数组被标记为变更

---

## Schema 版本演进策略

当前 Schema 为 **v1**。当需要扩展时，遵循以下原则：

1. **只加字段不改名**：已有字段名称和类型保持向后兼容，新需求通过新增字段解决
2. **可选优先**：新增字段一律为 optional，旧版 YAML 在不含该字段时解析器不应报错
3. **meta.version 同步递增**：每次 Schema 更新同步递增 `meta.version`，下游工具据此选择解析器版本

示例——未来可能的扩展：

```yaml
# v2 扩展：分镜信息（当前预留）
scenes:
  - scene_number: 1
    # ... 现有字段不变 ...
    shots:              # 新增，optional
      - shot_number: 1
        framing: "中景"
        camera_motion: "推"
```

---

## 增量生成场景下的 Schema 约定

当 pipeline 以增量模式运行（仅部分章节变更），YAML 组装遵循的合并规则：

1. **已有角色不重复**：新章节引入的角色追加到 `characters` 列表末尾，已有角色不修改
2. **已有场景不修改**：`acts[].scenes[]` 中属于未变更章节的场景保持原样
3. **新场景追加到所属幕**：新章节生成的场景追加到对应幕的 `scenes` 末尾
4. **跨章节场景**：如果一个场景跨越新旧章节（理论上极罕见），整场重新生成

这些约定不在 Schema 本身中显式表达，而是通过 YAML Assembler Agent 的 system prompt 和 pipeline 中的 `previous_yaml` 截断传递机制保障。

---

## 消费端映射

| 消费端 | 使用方式 |
|--------|----------|
| Monaco Editor | 直接加载 `.yaml` 文本，支持语法高亮和折叠 |
| 版本 Diff | 使用 YAML 文本的逐行 diff，而非解析后 diff |
| PDF/Word 导出 | Pandoc 读取 YAML 结构，按 `acts → scenes → content` 渲染为章节式文档 |
| 未来 TTS 配音 | 遍历 `content` 中 `type=dialogue` 的条目，通过 `character_id` 查找 `voice_trait` |
| 未来分镜生成 | 读取 `scenes[].location/time/atmosphere` 生成画面描述 prompt |

---

## 本项目功能对照

| 用户需求 | 实现状态 | 对应实现 |
|----------|----------|----------|
| 3 章节以上小说自动转换 | 已实现 | `pipeline.py` 中 `run_pipeline_structured()` 按每 10 章分组为幕，分批 AI 处理 |
| 结构化剧本 YAML 输出 | 已实现 | 7 Agent 流水线：分章→角色→世界观→剧情→场景→对白→YAML 组装 |
| 可编辑剧本初稿 | 已实现 | Monaco Editor 直接编辑 YAML |
| 版本管理 | 已实现 | `pro_script_version` 表 + MinIO 多版本存储 |
| 增量生成（仅处理新增/修改章节） | 已实现 | 内容哈希比对 + `unchanged_refs` 跳过不变章节 |
| 多格式导出（PDF/Word） | 规划中 | Pandoc 管道预留 |
| TTS 配音 | 规划中 | `voice_trait` 字段已预留在 schema 中 |
