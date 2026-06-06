import json
from loguru import logger
from minio import Minio
from app.config import settings


class NovelStorageClient:
    """Reads novel chapter data from MinIO object storage."""

    def __init__(self):
        self.client = Minio(
            endpoint=settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure,
        )
        self.bucket = settings.minio_bucket

    def read_chapters(self, minio_key: str) -> list[dict]:
        """
        Download and parse the chapters JSON file from MinIO.

        Returns:
            [{"chapterNo": 1, "title": "...", "content": "...", "wordCount": N}, ...]
        """
        try:
            response = self.client.get_object(self.bucket, minio_key)
            data = json.loads(response.read())
            response.close()
            response.release_conn()
            chapters = data.get("chapters", [])
            logger.info(f"Loaded {len(chapters)} chapters from MinIO: {minio_key}")
            return chapters
        except Exception as e:
            logger.error(f"Failed to read from MinIO: {minio_key}: {e}")
            raise
