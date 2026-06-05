from app.agents.base import BaseAgent


class SceneCutterAgent(BaseAgent):
    """Agent 5: Cut plot units into specific scenes."""

    def system_prompt(self) -> str:
        return """你是一个专业的场景切割助手。将每个情节单元切割为具体的拍摄场景。
对每个场景提取：
- scene_number: 场景号
- title: 场景标题
- location: 场景地点（格式：内景/外景-具体地点）
- time: 时间（日/夜/黄昏/黎明）
- atmosphere: 环境氛围
- present_char: 出场角色ID列表

以 JSON 格式输出：
[{"scene_number": 1, "title": "...", "location": "内景-房间", "time": "日", "atmosphere": "宁静", "present_char": ["char_001"]}, ...]"""

    def user_prompt(self, input_data: str) -> str:
        return f"请将以下情节单元切割为具体场景：\n\n{input_data}"
