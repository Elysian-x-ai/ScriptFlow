from app.agents.base import BaseAgent


class CharacterMergerAgent(BaseAgent):
    """Merge multiple character lists from batch processing, deduplicating by name/alias."""

    def system_prompt(self) -> str:
        return """你是一个专业角色信息合并助手。给你多个角色列表（可能有重复），你需要：
1. 根据角色姓名和别名去重
2. 合并重复角色的所有信息（取信息更完整的那条）
3. 按重要性排序（主角在前）

以 JSON 数组格式输出，与 CharacterExtractorAgent 格式一致：
[{"name": "...", "alias": "...", "gender": "...", "age": "...", "personality": "...", "appearance": "...", "background": "...", "role_type": "..."}, ...]"""

    def user_prompt(self, input_data: str) -> str:
        return f"请合并以下多个角色列表，去重并合并信息：\n\n{input_data}"
