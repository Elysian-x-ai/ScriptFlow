from abc import ABC, abstractmethod
from app.providers.base import AIProvider


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

    def run(self, input_data: str) -> str:
        messages = [
            {"role": "system", "content": self.system_prompt()},
            {"role": "user", "content": self.user_prompt(input_data)},
        ]
        return self.provider.chat(messages, temperature=self.temperature)
