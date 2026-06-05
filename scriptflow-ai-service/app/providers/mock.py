from typing import Optional
from app.providers.base import AIProvider


MOCK_SCRIPT_YAML = """meta:
  title: "\\u9b54\\u6cd5\\u4e16\\u754c\\u7684\\u5192\\u9669"
  source_novel: "\\u9b54\\u6cd5\\u4e16\\u754c\\u7684\\u5192\\u9669"
  author: "ScriptFlow AI"
  version: "v1.0"
  create_time: "2026-06-05T12:00:00"

characters:
  - id: "char_001"
    name: "\\u7f57\\u5c14"
    description: "\\u52c7\\u6562\\u7684\\u5c11\\u5e74\\u65c5\\u5f92\\uff0c\\u6027\\u683c\\u5766\\u7387\\u70ed\\u60c5\\uff0c\\u6000\\u63e3\\u7740\\u6210\\u4e3a\\u5723\\u9a91\\u58eb\\u7684\\u68a6\\u60f3"
    voice_trait: "\\u6e05\\u6717\\u5c11\\u5e74\\u97f3"
  - id: "char_002"
    name: "\\u5c0f\\u53ef"
    description: "\\u7cbe\\u7075\\u65cf\\u7684\\u9b54\\u6cd5\\u4f7f\\uff0c\\u7cbe\\u901a\\u53e4\\u8001\\u9b54\\u6cd5\\uff0c\\u6027\\u683c\\u4f18\\u96c5\\u800c\\u795e\\u79d8"
    voice_trait: "\\u6e29\\u67d4\\u5973\\u6027\\u97f3"

acts:
  - act_id: "act_01"
    title: "\\u7b2c\\u4e00\\u5e55\\uff1a\\u51fa\\u53d1"
    scenes:
      - scene_number: 1
        title: "\\u6751\\u5b50\\u7684\\u65e9\\u6668"
        location: "\\u5185\\u666f-\\u7f57\\u5c14\\u5bb6"
        time: "\\u65e5"
        atmosphere: "\\u5b81\\u9759\\u800c\\u6e29\\u99a8"
        present_char: ["char_001"]
        content:
          - type: action
            text: "\\u7f57\\u5c14\\u5728\\u65e9\\u6668\\u7684\\u9633\\u5149\\u4e2d\\u9192\\u6765\\uff0c\\u6536\\u62fe\\u7740\\u81ea\\u5df1\\u7684\\u5305\\u8896\\u3002\\u4ed6\\u77e5\\u9053\\uff0c\\u4eca\\u5929\\u5c31\\u662f\\u51fa\\u53d1\\u7684\\u65e5\\u5b50\\u3002"
          - type: dialogue
            character_id: "char_001"
            text: "\\u7ec8\\u4e8e\\u8981\\u5f00\\u59cb\\u4e86\\uff0c\\u6211\\u7684\\u5192\\u9669\\uff01"
      - scene_number: 2
        title: "\\u6751\\u53e3\\u7684\\u9047\\u89c1"
        location: "\\u5916\\u666f-\\u6751\\u53e3\\u5c0f\\u5f84"
        time: "\\u65e5"
        atmosphere: "\\u5fae\\u98ce\\u6b63\\u597d"
        present_char: ["char_001", "char_002"]
        content:
          - type: action
            text: "\\u7f57\\u5c14\\u8d70\\u5230\\u6751\\u53e3\\uff0c\\u9067\\u9065\\u770b\\u89c1\\u4e00\\u4e2a\\u7a7f\\u7740\\u6d41\\u661f\\u7eb9\\u62ab\\u98ce\\u7684\\u4eba\\u5f71\\u3002"
          - type: dialogue
            character_id: "char_002"
            text: "\\u4f60\\u5c31\\u662f\\u90a3\\u4e2a\\u8981\\u53bb\\u5192\\u9669\\u7684\\u5c0f\\u5bb6\\u4f19\\u5417\\uff1f\\u5de7\\u4e86\\uff0c\\u6211\\u4e5f\\u8981\\u53bb\\u90a3\\u4e2a\\u65b9\\u5411\\u3002"
"""


class MockProvider(AIProvider):
    @property
    def name(self) -> str:
        return "Mock"

    def chat(
        self,
        messages: list[dict],
        temperature: float = 0.7,
        max_tokens: Optional[int] = None,
    ) -> str:
        # Return mock content based on the last user message
        last = messages[-1]["content"] if messages else ""
        last_lower = last.lower()

        if "yaml" in last_lower or "assembl" in last_lower:
            return MOCK_SCRIPT_YAML
        elif "dialogue" in last_lower or "对白" in last:
            return '{"scenes": [{"scene_number": 1, "content": [{"type": "dialogue", "character_id": "char_001", "text": "你好"}]}]}'
        elif "scene" in last_lower or "场景" in last:
            return '[{"scene_number": 1, "title": "村庄的早晨", "location": "内景-罗尔家", "time": "日", "atmosphere": "宁静而温馨"}]'
        elif "plot" in last_lower or "剧情" in last:
            return '[{"name": "出发", "description": "主角离开村庄踏上冒险", "chapters": [1]}]'
        elif "world" in last_lower or "世界观" in last:
            return '{"setting": "剑与魔法的中世纪奇幻世界", "locations": ["村庄", "森林", "城堡"], "rules": ["魔法需要吟唱"]}'
        elif "character" in last_lower or "角色" in last:
            return '[{"name": "罗尔", "gender": "男", "age": "16", "personality": "勇敢热情", "appearance": "褐色短发", "background": "农家少年", "role_type": "主角"}]'
        elif "chapter" in last_lower or "章节" in last:
            return '[{"chapter_no": 1, "title": "启程", "content": "这是一个关于冒险的故事..."}]'
        else:
            return MOCK_SCRIPT_YAML
