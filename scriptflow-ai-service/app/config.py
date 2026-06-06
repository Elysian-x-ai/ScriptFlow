from pydantic_settings import BaseSettings
from typing import Literal


class Settings(BaseSettings):
    # AI Provider
    ai_provider: Literal["claude", "deepseek", "openai", "mock"] = "deepseek"
    claude_api_key: str = ""
    deepseek_api_key: str = "sk-7d583ad01e774e9cb070c3dad8108e83"
    openai_api_key: str = ""

    # RabbitMQ
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "guest"
    rabbitmq_pass: str = "guest"

    # Queue names
    queue_task: str = "scriptflow.task.submit"
    queue_result: str = "scriptflow.task.result"
    queue_log: str = "scriptflow.task.log"

    # Server
    host: str = "0.0.0.0"
    port: int = 8001
    log_level: str = "INFO"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
