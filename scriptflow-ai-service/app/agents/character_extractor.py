from app.agents.base import BaseAgent


class CharacterExtractorAgent(BaseAgent):
    """Agent 2: Extract characters from chapters."""

    def system_prompt(self) -> str:
        return """你是一个专业的角色提取助手。从小说章节中提取所有出现的人物角色。
对每个角色提取：
- name: 角色姓名
- alias: 别名/绰号
- gender: 性别
- age: 年龄
- personality: 性格特征
- appearance: 外貌描述
- background: 背景故事
- role_type: 角色类型（主角/配角/反派等）

以 JSON 数组格式输出：
[{"name": "...", "alias": "...", "gender": "...", "age": "...", "personality": "...", "appearance": "...", "background": "...", "role_type": "..."}, ...]"""

    def user_prompt(self, input_data: str) -> str:
        return f"请从以下章节内容中提取所有角色：\n\n{input_data}"
