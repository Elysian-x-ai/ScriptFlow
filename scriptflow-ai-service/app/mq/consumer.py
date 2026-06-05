import json
from typing import Callable, Optional
from loguru import logger
import aio_pika
from app.config import settings


class TaskConsumer:
    """Consumes tasks from the RabbitMQ submit queue."""

    def __init__(self, handler: Callable):
        """
        Args:
            handler: async callable(task_id, task_type, project_id, params, user_id)
        """
        self.handler = handler
        self.connection: Optional[aio_pika.Connection] = None

    async def start(self):
        self.connection = await aio_pika.connect_robust(
            host=settings.rabbitmq_host,
            port=settings.rabbitmq_port,
            login=settings.rabbitmq_user,
            password=settings.rabbitmq_pass,
        )
        channel = await self.connection.channel()
        await channel.set_qos(prefetch_count=1)

        queue = await channel.declare_queue(settings.queue_task, durable=True)
        logger.info(f"Listening on queue: {settings.queue_task}")

        async with queue.iterator() as queue_iter:
            async for message in queue_iter:
                async with message.process():
                    try:
                        body = json.loads(message.body.decode())
                        task_id = int(body.get("taskId", 0))
                        task_type = body.get("taskType", "")
                        project_id = int(body.get("projectId", 0))
                        params = body.get("params")
                        user_id = int(body.get("userId", 0))

                        logger.info(f"Received task: id={task_id}, type={task_type}, project={project_id}")
                        await self.handler(task_id, task_type, project_id, params, user_id)
                    except Exception as e:
                        logger.error(f"Failed to process message: {e}")

    async def close(self):
        if self.connection:
            await self.connection.close()
            logger.info("RabbitMQ consumer connection closed")
