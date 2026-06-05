package com.scriptflow.storage.config;

import com.scriptflow.framework.properties.StorageProperties;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO client configuration.
 */
@Configuration
@ConditionalOnProperty(prefix = "scriptflow.storage", name = "type", havingValue = "minio", matchIfMissing = true)
@RequiredArgsConstructor
public class MinioConfig {

    private final StorageProperties storageProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(storageProperties.getEndpoint())
                .credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey())
                .region(storageProperties.getRegion())
                .build();
    }
}
