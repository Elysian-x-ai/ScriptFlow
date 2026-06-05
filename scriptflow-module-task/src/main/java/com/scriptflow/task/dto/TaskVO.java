package com.scriptflow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Task view object.
 */
@Data
@Schema(description = "Task view object")
public class TaskVO {

    @Schema(description = "Task ID")
    private Long id;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Task type")
    private String taskType;

    @Schema(description = "Status: 0=pending, 1=processing, 2=completed, 3=failed, 4=cancelled")
    private Integer status;

    @Schema(description = "Progress (0-100)")
    private Integer progress;

    @Schema(description = "Error message")
    private String errorMsg;

    @Schema(description = "Start time")
    private LocalDateTime startTime;

    @Schema(description = "Finish time")
    private LocalDateTime finishTime;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;
}
