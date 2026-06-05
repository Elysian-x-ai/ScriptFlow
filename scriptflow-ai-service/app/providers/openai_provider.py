from typing import Optional
from openai import OpenAI
from app.providers.base import AIProvider
from app.config import settings


class OpenAIProvider(AIProvider):
    def __init__(self):
        self.client = OpenAI(api_key=settings.openai_api_key)
        self.model = "gpt-4o"

    @property
    def name(self) -> str:
        return f"OpenAI({self.model})"

    def chat(
        self,
        messages: list[dict],
        temperature: float = 0.7,
        max_tokens: Optional[int] = None,
    ) -> str:
        resp = self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            temperature=temperature,
            max_tokens=max_tokens or 4096,
        )
        return resp.choices[0].message.content or ""
