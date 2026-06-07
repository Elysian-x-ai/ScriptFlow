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

    def read_novel_doc(self, minio_key: str) -> dict:
        """
        Download and parse the full novel document JSON from MinIO.

        Returns:
            {"projectId": ..., "chapters": [...], "previousYaml": "..."}
        """
        try:
            response = self.client.get_object(self.bucket, minio_key)
            data = json.loads(response.read())
            response.close()
            response.release_conn()
            logger.info(f"Loaded novel doc from MinIO: {minio_key}")
            return data
        except Exception as e:
            logger.error(f"Failed to read from MinIO: {minio_key}: {e}")
            raise

    def read_chapters(self, minio_key: str) -> list[dict]:
        """Backward-compatible wrapper; prefer read_novel_doc for full data."""
        return self.read_novel_doc(minio_key).get("chapters", [])
