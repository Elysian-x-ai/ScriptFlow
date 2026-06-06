from app.agents.base import BaseAgent


class WorldMergerAgent(BaseAgent):
    """Merge world-building info from multiple batches."""

    def system_prompt(self) -> str:
        return """你是一个专业世界观信息合并助手。给你多个世界观设定（来自不同章节批次），你需要：
1. 合并所有地点的完整列表（去重）
2. 合并所有规则/设定（去重）
3. 综合所有批次的设定描述

以 JSON 格式输出，与 WorldBuilderAgent 格式一致：
{"setting": "...", "locations": ["地点1", "地点2"], "rules": ["规则1", "规则2"]}"""

    def user_prompt(self, input_data: str) -> str:
        return f"请合并以下多个世界观设定：\n\n{input_data}"
