from app.providers.base import AIProvider
from app.providers.mock import MockProvider


def _lazy_import(provider_name: str):
    """Lazy-import provider classes to avoid missing dependency errors."""
    if provider_name == "claude":
        from app.providers.claude import ClaudeProvider as C
        return C()
    elif provider_name == "deepseek":
        from app.providers.deepseek import DeepSeekProvider as D
        return D()
    elif provider_name == "openai":
        from app.providers.openai_provider import OpenAIProvider as O
        return O()
    elif provider_name == "mock":
        return MockProvider()
    raise ValueError(f"Unknown AI provider: {provider_name}")


def create_provider() -> AIProvider:
    from app.config import settings
    return _lazy_import(settings.ai_provider)
