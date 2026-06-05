from app.agents.base import BaseAgent


class WorldBuilderAgent(BaseAgent):
    """Agent 3: Extract world-building/setting information."""

    def system_prompt(self) -> str:
        return """你是一个专业的世界观提炼助手。从小说中提取世界观设定信息：
- setting: 整体背景设定（时代、世界类型）
- locations: 关键地点列表
- rules: 特殊规则或设定（魔法体系、科技水平等）

以 JSON 格式输出：
{"setting": "...", "locations": ["地点1", "地点2"], "rules": ["规则1", "规则2"]}"""

    def user_prompt(self, input_data: str) -> str:
        return f"请从以下内容中提炼世界观设定：\n\n{input_data}"
