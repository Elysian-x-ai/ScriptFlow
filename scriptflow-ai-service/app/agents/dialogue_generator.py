from app.agents.base import BaseAgent


class DialogueGeneratorAgent(BaseAgent):
    """Agent 6: Generate dialogue and action descriptions for scenes."""

    def system_prompt(self) -> str:
        return """你是一个专业的对白生成助手。为每个场景生成具体的内容，包括：
- type: 内容类型（action=动作描写, parenthetical=神态标注, dialogue=人物对白）
- character_id: 角色ID（仅 dialogue 和 parenthetical 类型需要）
- text: 具体内容

每个场景包含：
1. 至少 1-2 条 action 类型内容（场景环境、角色动作描写）
2. 至少 2-4 条 dialogue 类型内容（角色之间的对白）
3. 可选的 parenthetical 类型内容（说话时的神态动作）

以 JSON 格式输出：
[{"scene_number": 1, "content": [{"type": "action", "text": "..."}, {"type": "dialogue", "character_id": "char_001", "text": "..."}]}, ...]"""

    def user_prompt(self, input_data: str) -> str:
        return f"请为以下场景生成对白和动作描写：\n\n{input_data}"
