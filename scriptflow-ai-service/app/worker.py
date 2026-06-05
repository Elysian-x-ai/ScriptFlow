import asyncio
import time
from loguru import logger

from app.config import settings
from app.providers import create_provider
from app.mq.consumer import TaskConsumer
from app.mq.publisher import ResultPublisher
from app.pipeline import run_pipeline, ProgressCallback


class ScriptFlowWorker:
    """Background worker that consumes RabbitMQ tasks and runs the AI pipeline."""

    def __init__(self):
        self.provider = create_provider()
        self.publisher = ResultPublisher()
        self.consumer = TaskConsumer(self.handle_task)

    async def handle_task(
        self,
        task_id: int,
        task_type: str,
        project_id: int,
        params: str | None,
        user_id: int,
    ):
        """Handle a single task from the queue."""
        logger.info(f"Processing task {task_id} ({task_type}) for project {project_id}")
        start_time = time.time()

        try:
            # Report processing start
            await self.publisher.publish_log(
                task_id, "pipeline", 1, f"开始处理: {task_type} (Provider: {self.provider.name})"
            )

            # Get novel content from params
            novel_content = params or "这是一个示例小说内容，用于演示 AI 剧本生成流程。在正式使用时，此处会传入真实的小说原文。"
            progress_cb = ProgressCallback()
            stage_times = {}

            def on_progress(stage_index: int, status: str, message: str = ""):
                stage_name = progress_cb.stages[stage_index]

                if status == "processing":
                    stage_times[stage_index] = time.time()
                    asyncio.ensure_future(
                        self.publisher.publish_log(task_id, stage_name, 1, f"正在{stage_name}...")
                    )
                elif status == "completed":
                    cost = int((time.time() - stage_times.get(stage_index, start_time)) * 1000)
                    asyncio.ensure_future(
                        self.publisher.publish_log(task_id, stage_name, 2, message or f"{stage_name}完成", cost)
                    )
                    # Update task progress
                    progress = int((stage_index + 1) / 7 * 100)
                    asyncio.ensure_future(
                        self.publisher.publish_result(task_id, 1, progress=progress)
                    )

            progress_cb.on_progress = on_progress

            # Run the pipeline
            success, yaml_output, error = run_pipeline(novel_content, self.provider, progress_cb)

            elapsed = int((time.time() - start_time) * 1000)

            if success:
                await self.publisher.publish_log(
                    task_id, "pipeline", 2, f"处理完成，耗时 {elapsed}ms", elapsed
                )
                await self.publisher.publish_result(task_id, 2, result=yaml_output)
                logger.info(f"Task {task_id} completed successfully ({elapsed}ms)")
            else:
                await self.publisher.publish_log(
                    task_id, "pipeline", 3, f"处理失败: {error}", elapsed
                )
                await self.publisher.publish_result(task_id, 3, error=error)
                logger.error(f"Task {task_id} failed: {error}")

        except Exception as e:
            logger.error(f"Task {task_id} crashed: {e}")
            await self.publisher.publish_result(task_id, 3, error=str(e))

    async def start(self):
        logger.info(f"Starting ScriptFlow AI Worker (provider={settings.ai_provider})")
        await self.publisher.connect()
        await self.consumer.start()

    async def shutdown(self):
        await self.publisher.close()
        await self.consumer.close()
