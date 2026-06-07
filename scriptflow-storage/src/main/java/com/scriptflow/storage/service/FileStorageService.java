package com.scriptflow.storage.service;

import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.storage.config.StorageProperties;
import com.scriptflow.storage.dto.MinioObjectItem;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * File storage service (MinIO implementation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(storageProperties.getBucketName()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(storageProperties.getBucketName()).build());
                log.info("Bucket '{}' created", storageProperties.getBucketName());
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket, storage unavailable", e);
            throw new RuntimeException("MinIO storage initialization failed", e);
        }
    }

    /**
     * Upload a file and return the object key.
     */
    public String upload(MultipartFile file, String directory) {
        try {
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectKey = directory + "/" + UUID.randomUUID() + extension;

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(storageProperties.getBucketName())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            minioClient.putObject(args);
            log.info("File uploaded: {} ({})", objectKey, file.getSize());
            return objectKey;
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "File upload failed: " + e.getMessage());
        }
    }

    /**
     * Upload raw bytes as a file.
     */
    public String uploadBytes(String objectKey, byte[] data, String contentType) {
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(storageProperties.getBucketName())
                    .object(objectKey)
                    .stream(stream, data.length, -1)
                    .contentType(contentType)
                    .build();
            minioClient.putObject(args);
            log.info("Bytes uploaded: {} ({} bytes)", objectKey, data.length);
            return objectKey;
        } catch (Exception e) {
            log.error("Bytes upload failed for {}", objectKey, e);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "Upload failed: " + e.getMessage());
        }
    }

    /**
     * Upload a string as a JSON file.
     */
    public String uploadString(String objectKey, String content) {
        return uploadBytes(objectKey, content.getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }

    /**
     * Read a file from MinIO and return its content as a UTF-8 string.
     */
    public String readString(String objectKey) {
        try {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(objectKey)
                            .build())) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Failed to read file from MinIO: {}", objectKey, e);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Delete all objects with the given prefix (e.g. a project directory).
     * Silently continues if no objects match. Logs individual delete failures.
     */
    public void removeByPrefix(String prefix) {
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            List<DeleteObject> toDelete = new LinkedList<>();
            for (Result<Item> result : objects) {
                Item item = result.get();
                toDelete.add(new DeleteObject(item.objectName()));
            }

            if (toDelete.isEmpty()) {
                log.info("No objects to delete with prefix: {}", prefix);
                return;
            }

            Iterable<Result<DeleteError>> errors = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .objects(toDelete)
                            .build());

            for (Result<DeleteError> errorResult : errors) {
                DeleteError error = errorResult.get();
                log.warn("Failed to delete {}: {}", error.objectName(), error.message());
            }

            log.info("Deleted {} objects with prefix: {}", toDelete.size(), prefix);
        } catch (Exception e) {
            log.error("Failed to remove objects by prefix: {}", prefix, e);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "Failed to clean up storage: " + e.getMessage());
        }
    }

    /**
     * List objects with the given prefix.
     */
    public List<MinioObjectItem> listObjects(String prefix) {
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            List<MinioObjectItem> result = new ArrayList<>();
            for (Result<Item> objectResult : objects) {
                Item item = objectResult.get();
                result.add(new MinioObjectItem(item.objectName(), item.size(), Date.from(item.lastModified().toInstant())));
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to list objects with prefix: {}", prefix, e);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "Failed to list objects: " + e.getMessage());
        }
    }

    /**
     * Delete a single file by object key.
     */
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(objectKey)
                            .build());
            log.info("File deleted: {}", objectKey);
        } catch (Exception e) {
            log.error("File delete failed: {}", objectKey, e);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "File delete failed");
        }
    }

    /**
     * Get a pre-signed URL for temporary access.
     */
    public String getPresignedUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", objectKey, e);
            throw new BusinessException(ResultCode.STORAGE_ERROR, "Failed to generate access URL");
        }
    }

    /**
     * Get file input stream.
     */
    public InputStream getFile(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            log.error("Failed to get file: {}", objectKey, e);
            throw new BusinessException(ResultCode.NOT_FOUND, "File not found");
        }
    }
}
