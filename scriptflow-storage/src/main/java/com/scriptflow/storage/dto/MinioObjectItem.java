package com.scriptflow.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * MinIO object metadata returned by list operations.
 */
@Data
@AllArgsConstructor
public class MinioObjectItem {
    private String objectName;
    private long size;
    private Date lastModified;
}
