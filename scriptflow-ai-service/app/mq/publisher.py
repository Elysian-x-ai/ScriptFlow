import json
from typing import Optional
from loguru import logger
import aio_pika
from app.config import settings


class ResultPublisher:
    """Publishes task results and logs back to RabbitMQ."""

    def __init__(self):
        self.connection: Optional[aio_pika.Connection] = None
        self.channel: Optional[aio_pika.Channel] = None

    async def connect(self):
        self.connection = await aio_pika.connect_robust(
            host=settings.rabbitmq_host,
            port=settings.rabbitmq_port,
            login=settings.rabbitmq_user,
            password=settings.rabbitmq_pass,
        )
        self.channel = await self.connection.channel()
        logger.info("Connected to RabbitMQ (publisher)")

    async def publish_result(self, task_id: int, status: int, result: Optional[str] = None, error: Optional[str] = None, progress: int = 100):
        if not self.channel:
            await self.connect()
        message = {
            "taskId": task_id,
            "status": status,
            "result": result,
            "error": error,
            "progress": progress,
        }
        await self.channel.default_exchange.publish(
            aio_pika.Message(body=json.dumps(message).encode()),
            routing_key=settings.queue_result,
        )
        logger.info(f"Published result for task {task_id}: status={status}")

    async def publish_log(
        self,
        task_id: int,
        stage: str,
        status: int,
        message: str,
        cost_time: Optional[int] = None,
    ):
        if not self.channel:
            await self.connect()
        log_msg = {
            "taskId": task_id,
            "stage": stage,
            "status": status,
            "message": message,
            "costTime": cost_time,
        }
        await self.channel.default_exchange.publish(
            aio_pika.Message(body=json.dumps(log_msg).encode()),
            routing_key=settings.queue_log,
        )

    async def close(self):
        if self.connection:
            await self.connection.close()
            self.channel = None
            self.connection = None
            logger.info("RabbitMQ publisher connection closed")
