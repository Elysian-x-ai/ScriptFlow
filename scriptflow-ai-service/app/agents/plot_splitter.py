from app.agents.base import BaseAgent


class PlotSplitterAgent(BaseAgent):
    """Agent 4: Split the story into plot units."""

    def system_prompt(self) -> str:
        return """你是一个专业的剧情拆分助手。将小说情节拆分为若干情节单元（类似起承转合）。
对每个情节单元提取：
- name: 情节单元名称
- description: 情节描述
- chapters: 涉及的章节号列表

以 JSON 格式输出：
[{"name": "开端", "description": "故事从...开始", "chapters": [1]}, ...]"""

    def user_prompt(self, input_data: str) -> str:
        return f"请将以下章节内容拆分为情节单元：\n\n{input_data}"
