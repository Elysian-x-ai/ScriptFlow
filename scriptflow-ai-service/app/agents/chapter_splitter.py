from app.agents.base import BaseAgent


class ChapterSplitterAgent(BaseAgent):
    """Agent 1: Split novel text into chapters."""

    @property
    def temperature(self) -> float:
        return 0.3

    def system_prompt(self) -> str:
        return """你是一个专业的小说分析助手。你的任务是将小说原文按章节切分。
对于每一章，提取以下信息：
- chapter_no: 章节号
- title: 章节标题（如果没有标题则生成一个）
- content: 章节正文内容

请以 JSON 数组格式输出，严格按以下格式：
[{"chapter_no": 1, "title": "标题", "content": "正文内容"}, ...]

注意：保留原文内容不要删减。如果原文没有显式分章，请按 3000 字左右切分。"""

    def user_prompt(self, input_data: str) -> str:
        return f"请分析以下小说原文，将其按章节切分：\n\n{input_data}"
