package com.scriptflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Script view object.
 */
@Data
@Schema(description = "Script view object")
public class ScriptVO {

    @Schema(description = "Script ID")
    private Long id;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Version number")
    private Integer version;

    @Schema(description = "YAML content")
    private String yamlContent;

    @Schema(description = "Word count")
    private Integer wordCount;

    @Schema(description = "Status: 0=draft, 1=generating, 2=completed, 3=failed")
    private Integer status;

    @Schema(description = "Error message")
    private String errorMsg;

    @Schema(description = "Current generation task ID (null if not generating)")
    private Long currentTaskId;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
