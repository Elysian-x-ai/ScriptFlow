import asyncio
import json
import time
from concurrent.futures import ThreadPoolExecutor
from loguru import logger

from app.config import settings
from app.providers import create_provider
from app.mq.consumer import TaskConsumer
from app.mq.publisher import ResultPublisher
from app.pipeline import run_pipeline, ProgressCallback
from app.agents.chapter_splitter import ChapterSplitterAgent
from app.agents.character_extractor import CharacterExtractorAgent
from app.agents.world_builder import WorldBuilderAgent


class ScriptFlowWorker:
    """Background worker that consumes RabbitMQ tasks and runs the AI pipeline."""

    def __init__(self):
        self.provider = create_provider()
        self.publisher = ResultPublisher()
        self.consumer = TaskConsumer(self.handle_task)
        self._executor = ThreadPoolExecutor(max_workers=1)
        self._loop: asyncio.AbstractEventLoop | None = None

    async def handle_task(
        self,
        task_id: int,
        task_type: str,
        project_id: int,
        params: str | None,
        user_id: int,
    ):
        """Route task to the appropriate handler based on task_type."""
        logger.info(f"Processing task {task_id} ({task_type}) for project {project_id}")
        self._loop = asyncio.get_event_loop()

        task_routing = {
            "script_generate": self._handle_generate,
            "novel_analysis": self._handle_analysis,
            "character_extract": self._handle_extract,
            "script_revise": self._handle_revise,
        }

        handler = task_routing.get(task_type)
        if handler is None:
            logger.warning(f"Unknown task type: {task_type}, falling back to full pipeline")
            handler = self._handle_generate

        await handler(task_id, task_type, project_id, params, user_id)

    async def _extract_novel_content(self, params: str | None) -> str:
        """Extract novel content from params JSON."""
        if not params:
            return ""
        try:
            parsed = json.loads(params)
            if isinstance(parsed, dict):
                return parsed.get("novelContent", "")
        except (json.JSONDecodeError, TypeError):
            pass
        return ""

    def _publish_sync(self, coro):
        """Schedule an async publisher coroutine from a synchronous callback."""
        if self._loop and self._loop.is_running():
            asyncio.run_coroutine_threadsafe(coro, self._loop)

    async def _handle_generate(
        self, task_id: int, task_type: str, project_id: int, params: str | None, user_id: int
    ):
        """Full 7-stage pipeline: novel -> structured YAML script."""
        start_time = time.time()
        novel_content = await self._extract_novel_content(params)

        await self.publisher.publish_log(
            task_id, "pipeline", 1,
            f"开始剧本生成 (Provider: {self.provider.name})"
        )

        if not novel_content:
            await self.publisher.publish_log(
                task_id, "pipeline", 3, "未找到小说内容，请先在章节管理中导入小说。"
            )
            await self.publisher.publish_result(task_id, 3, error="缺少小说内容")
            return

        # Build progress callback that publishes via the running event loop
        progress_cb = ProgressCallback()
        stage_times: dict[int, float] = {}

        def on_progress(stage_index: int, status: str, message: str = ""):
            stage_name = progress_cb.stages[stage_index]

            if status == "processing":
                stage_times[stage_index] = time.time()
                self._publish_sync(
                    self.publisher.publish_log(task_id, stage_name, 1, f"正在{stage_name}...")
                )

            elif status == "completed":
                cost = int((time.time() - stage_times.get(stage_index, start_time)) * 1000)
                self._publish_sync(
                    self.publisher.publish_log(task_id, stage_name, 2, message or f"{stage_name}完成", cost)
                )
                progress = int((stage_index + 1) / len(progress_cb.stages) * 100)
                self._publish_sync(
                    self.publisher.publish_result(task_id, 1, progress=progress)
                )

        progress_cb.on_progress = on_progress

        # Run the synchronous pipeline in a thread to keep event loop free
        loop = self._loop or asyncio.get_event_loop()
        success, yaml_output, error = await loop.run_in_executor(
            self._executor, run_pipeline, novel_content, self.provider, progress_cb
        )

        elapsed = int((time.time() - start_time) * 1000)

        if success:
            await self.publisher.publish_log(
                task_id, "pipeline", 2, f"剧本生成完成，耗时 {elapsed}ms", elapsed
            )
            await self.publisher.publish_result(task_id, 2, result=yaml_output)
            logger.info(f"Task {task_id} completed successfully ({elapsed}ms)")
        else:
            await self.publisher.publish_log(
                task_id, "pipeline", 3, f"生成失败: {error}", elapsed
            )
            await self.publisher.publish_result(task_id, 3, error=error)
            logger.error(f"Task {task_id} failed: {error}")

    async def _handle_analysis(
        self, task_id: int, task_type: str, project_id: int, params: str | None, user_id: int
    ):
        """Analysis pipeline: chapter split + character extract + world building."""
        start_time = time.time()
        novel_content = await self._extract_novel_content(params)

        await self.publisher.publish_log(
            task_id, "analysis", 1,
            f"开始小说分析 (Provider: {self.provider.name})"
        )

        if not novel_content:
            await self.publisher.publish_log(
                task_id, "analysis", 3, "未找到小说内容"
            )
            await self.publisher.publish_result(task_id, 3, error="缺少小说内容")
            return

        try:
            # Stage 1: Chapter split
            await self.publisher.publish_log(task_id, "章节切分", 1, "正在分章...")
            splitter = ChapterSplitterAgent(self.provider)
            chapters = splitter.run(novel_content)
            await self.publisher.publish_log(task_id, "章节切分", 2, "章节切分完成", 0)
            await self.publisher.publish_result(task_id, 1, progress=33)

            # Stage 2: Character extract
            await self.publisher.publish_log(task_id, "角色抽取", 1, "正在提取角色...")
            extractor = CharacterExtractorAgent(self.provider)
            characters = extractor.run(chapters)
            await self.publisher.publish_log(task_id, "角色抽取", 2, "角色提取完成", 0)
            await self.publisher.publish_result(task_id, 1, progress=66)

            # Stage 3: World building
            await self.publisher.publish_log(task_id, "世界观提炼", 1, "正在提炼世界观...")
            builder = WorldBuilderAgent(self.provider)
            world_info = builder.run(chapters)
            await self.publisher.publish_log(task_id, "世界观提炼", 2, "世界观提炼完成", 0)
            await self.publisher.publish_result(task_id, 1, progress=100)

            analysis_result = json.dumps({
                "chapters": json.loads(splitter.parse_json(chapters)),
                "characters": json.loads(extractor.parse_json(characters)),
                "world_info": json.loads(builder.parse_json(world_info)),
            }, ensure_ascii=False, indent=2)

            elapsed = int((time.time() - start_time) * 1000)
            await self.publisher.publish_log(
                task_id, "analysis", 2, f"分析完成，耗时 {elapsed}ms", elapsed
            )
            await self.publisher.publish_result(task_id, 2, result=analysis_result)

        except Exception as e:
            logger.exception(f"Analysis failed: {e}")
            await self.publisher.publish_result(task_id, 3, error=str(e))

    async def _handle_extract(
        self, task_id: int, task_type: str, project_id: int, params: str | None, user_id: int
    ):
        """Character extraction only."""
        start_time = time.time()
        novel_content = await self._extract_novel_content(params)

        if not novel_content:
            await self.publisher.publish_result(task_id, 3, error="缺少小说内容")
            return

        try:
            extractor = CharacterExtractorAgent(self.provider)
            characters = extractor.run(novel_content)
            elapsed = int((time.time() - start_time) * 1000)

            result_data = json.dumps(
                json.loads(extractor.parse_json(characters)),
                ensure_ascii=False, indent=2
            )

            await self.publisher.publish_log(
                task_id, "character_extract", 2, f"角色提取完成，耗时 {elapsed}ms", elapsed
            )
            await self.publisher.publish_result(task_id, 2, result=result_data)

        except Exception as e:
            logger.exception(f"Extract failed: {e}")
            await self.publisher.publish_result(task_id, 3, error=str(e))

    async def _handle_revise(
        self, task_id: int, task_type: str, project_id: int, params: str | None, user_id: int
    ):
        """Script revision: modify existing YAML based on user instruction."""
        start_time = time.time()

        await self.publisher.publish_log(
            task_id, "revision", 1, "开始处理修改请求..."
        )

        instruction = ""
        novel_content = ""
        if params:
            try:
                parsed = json.loads(params)
                if isinstance(parsed, dict):
                    instruction = parsed.get("instruction", "")
                    novel_content = parsed.get("novelContent", "")
            except (json.JSONDecodeError, TypeError):
                pass

        if not novel_content and not instruction:
            await self.publisher.publish_result(task_id, 2, result="请提供修改指令或小说内容。")
            return

        if instruction and not novel_content:
            result_text = f"收到修改请求：{instruction}\n\n请先在章节管理中添加小说内容，然后使用「生成剧本」功能生成完整剧本后再进行修改。"
            await self.publisher.publish_result(task_id, 2, result=result_text)
        else:
            await self._handle_generate(task_id, "script_generate", project_id, params, user_id)

        elapsed = int((time.time() - start_time) * 1000)
        logger.info(f"Revision task {task_id} done ({elapsed}ms)")

    async def start(self):
        logger.info(f"Starting ScriptFlow AI Worker (provider={settings.ai_provider})")
        await self.publisher.connect()
        await self.consumer.start()

    async def shutdown(self):
        await self.publisher.close()
        await self.consumer.close()
