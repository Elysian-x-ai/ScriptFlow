package com.scriptflow.project.dto;

import lombok.Data;

/**
 * YAML file metadata from MinIO storage.
 */
@Data
public class MinioYamlVO {
    private String objectKey;
    private Integer version;
    private Long fileSize;
    private String lastModified;
    private Long projectId;
    private Long scriptId;
}
