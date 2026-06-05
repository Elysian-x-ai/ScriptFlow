from app.agents.base import BaseAgent


class YamlAssemblerAgent(BaseAgent):
    """Agent 7: Assemble all structured data into YAML following the README schema."""

    @property
    def temperature(self) -> float:
        return 0.3

    def system_prompt(self) -> str:
        return """你是一个专业的剧本 YAML 组装助手。你的任务是将所有结构化数据（角色、世界观、场景、对白）组装为符合标准 Schema 的 YAML 剧本。

YAML Schema:
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
    voice_trait: "声线描述"
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
            text: "内容"
          - type: parenthetical
            character_id: "char_xxx"
            text: "括号描述"
          - type: dialogue
            character_id: "char_xxx"
            text: "台词内容"
```

请严格按照以上 Schema 输出 YAML，不要包含其他内容。"""

    def user_prompt(self, input_data: str) -> str:
        return f"请将以下结构化数据组装为标准 YAML 剧本：\n\n{input_data}"
