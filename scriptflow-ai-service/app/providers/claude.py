from typing import Optional
from anthropic import Anthropic
from app.providers.base import AIProvider
from app.config import settings


class ClaudeProvider(AIProvider):
    def __init__(self):
        self.client = Anthropic(api_key=settings.claude_api_key)
        self.model = "claude-sonnet-4-20250514"

    @property
    def name(self) -> str:
        return f"Claude({self.model})"

    def chat(
        self,
        messages: list[dict],
        temperature: float = 0.7,
        max_tokens: Optional[int] = None,
    ) -> str:
        system = ""
        filtered = []
        for m in messages:
            if m.get("role") == "system":
                system += m.get("content", "") + "\n"
            else:
                filtered.append(m)

        kwargs = dict(
            model=self.model,
            max_tokens=max_tokens or 4096,
            temperature=temperature,
            messages=filtered,
        )
        if system.strip():
            kwargs["system"] = system.strip()

        resp = self.client.messages.create(**kwargs)
        if not resp.content:
            raise RuntimeError(f"Claude API returned empty content for model {self.model}")
        block = resp.content[0]
        if not hasattr(block, "text"):
            raise RuntimeError(f"Claude returned unexpected content block type: {type(block).__name__}")
        return block.text
