package com.scriptflow.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * File storage properties (MinIO / OSS).
 */
@Data
@Component
@ConfigurationProperties(prefix = "scriptflow.storage")
public class StorageProperties {

    private String type = "minio";
    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketName = "scriptflow";
    private String region = "cn-east-1";
}
