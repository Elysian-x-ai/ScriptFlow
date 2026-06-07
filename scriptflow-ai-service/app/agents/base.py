import json
import re
from abc import ABC, abstractmethod
from app.providers.base import AIProvider


def strip_markdown(text: str) -> str:
    """Strip markdown code block fences (```json ... ```) from text."""
    text = text.strip()
    # Remove opening ```json, ```, or other ```xxx markers
    text = re.sub(r'^```\w*\s*\n?', '', text)
    # Remove closing ```
    text = re.sub(r'\n?```\s*$', '', text)
    return text.strip()


class BaseAgent(ABC):
    """Base class for a pipeline agent that processes novel content."""

    def __init__(self, provider: AIProvider):
        self.provider = provider

    @abstractmethod
    def system_prompt(self) -> str:
        ...

    @abstractmethod
    def user_prompt(self, input_data: str) -> str:
        ...

    @property
    def temperature(self) -> float:
        return 0.7

    @property
    def max_retries(self) -> int:
        """Number of retries when output is not valid JSON."""
        return 2

    @property
    def max_tokens(self) -> int | None:
        """Maximum output tokens for this agent. None = use provider default."""
        return None

    def parse_json(self, text: str):
        """
        Try to parse JSON from LLM output.
        Handles markdown code blocks, leading/trailing text, and retries.
        """
        text = strip_markdown(text)

        # Direct parse
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            pass

        # Try to find content between first [ or { and matching closing bracket
        for start, end in [("[", "]"), ("{", "}")]:
            start_idx = text.find(start)
            if start_idx >= 0:
                # Find the matching closing bracket
                depth = 0
                for i in range(start_idx, len(text)):
                    ch = text[i]
                    if ch == start[0]:
                        depth += 1
                    elif ch == end[0]:
                        depth -= 1
                        if depth == 0:
                            try:
                                return json.loads(text[start_idx : i + 1])
                            except json.JSONDecodeError:
                                break
        raise ValueError(f"Cannot parse JSON from agent output: {text[:200]}")

    def run(self, input_data: str) -> str:
        """
        Run the agent and return cleaned output.

        For JSON-output agents: validates JSON, retries on failure,
        returns re-serialized JSON string (without markdown wrapping).
        For YAML-output agents (YamlAssembler): returns raw text.
        """
        is_json_agent = self.__class__.__name__ != "YamlAssemblerAgent"
        last_error = ""

        for attempt in range(self.max_retries + 1):
            user_msg = self.user_prompt(input_data)
            if last_error:
                user_msg += f"\n\n上一轮输出不是有效的 JSON 格式：{last_error}\n请只返回 JSON 数据，不要使用 markdown 包裹。"
            messages = [
                {"role": "system", "content": self.system_prompt()},
                {"role": "user", "content": user_msg},
            ]
            raw = self.provider.chat(messages, temperature=self.temperature, max_tokens=self.max_tokens)

            if is_json_agent:
                try:
                    parsed = self.parse_json(raw)
                except ValueError as e:
                    last_error = str(e)
                    continue
                # Return clean, re-serialized JSON string
                return json.dumps(parsed, ensure_ascii=False)
            else:
                return raw

        # All retries exhausted — return cleaned raw text
        cleaned = strip_markdown(raw) if is_json_agent else raw
        return cleaned
