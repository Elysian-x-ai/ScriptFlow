import asyncio
from contextlib import asynccontextmanager
from loguru import logger
from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional

from app.config import settings
from app.worker import ScriptFlowWorker


worker: Optional[ScriptFlowWorker] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global worker
    logger.info(f"Starting ScriptFlow AI Service on {settings.host}:{settings.port}")
    logger.info(f"AI Provider: {settings.ai_provider}")

    worker = ScriptFlowWorker()
    # Start the RabbitMQ consumer in background
    async def run_worker():
        try:
            await worker.start()
        except Exception as e:
            logger.error(f"Worker failed to start: {e}")

    bg_task = asyncio.create_task(run_worker())

    yield

    # Shutdown
    logger.info("Shutting down...")
    if worker:
        await worker.shutdown()
    bg_task.cancel()
    try:
        await bg_task
    except asyncio.CancelledError:
        pass


app = FastAPI(
    title="ScriptFlow AI Service",
    description="Multi-agent AI pipeline for novel-to-script conversion",
    version="1.0.0",
    lifespan=lifespan,
)


class TaskSubmitRequest(BaseModel):
    task_id: int
    task_type: str
    project_id: int
    params: Optional[str] = None
    user_id: int = 0


class TaskSubmitResponse(BaseModel):
    success: bool
    task_id: int
    message: str = ""


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "provider": settings.ai_provider,
    }


@app.get("/api/v1/tasks/{task_id}")
async def get_task_status(task_id: int):
    """Poll task status (placeholder — actual status tracked in Java backend)."""
    return {
        "task_id": task_id,
        "status": "processing",
        "message": "Task is being processed by the worker",
    }


@app.post("/api/v1/tasks")
async def submit_task_direct(request: TaskSubmitRequest):
    """Direct task submission (bypasses RabbitMQ)."""
    global worker
    if worker is None:
        return TaskSubmitResponse(success=False, task_id=request.task_id, message="Worker not initialized")

    async def run_task():
        try:
            await worker.handle_task(
                task_id=request.task_id,
                task_type=request.task_type,
                project_id=request.project_id,
                params=request.params,
                user_id=request.user_id,
            )
        except Exception as e:
            logger.error(f"Direct task {request.task_id} handler failed: {e}")

    asyncio.create_task(run_task())
    return TaskSubmitResponse(success=True, task_id=request.task_id, message="Task submitted")


def main():
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        log_level=settings.log_level.lower(),
    )


if __name__ == "__main__":
    main()
