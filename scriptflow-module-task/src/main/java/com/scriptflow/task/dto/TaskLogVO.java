package com.scriptflow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Task log view object.
 */
@Data
@Schema(description = "Task log view object")
public class TaskLogVO {

    @Schema(description = "Log ID")
    private Long id;

    @Schema(description = "Task ID")
    private Long taskId;

    @Schema(description = "Processing stage")
    private String stage;

    @Schema(description = "Stage status")
    private Integer status;

    @Schema(description = "Log message")
    private String message;

    @Schema(description = "Time cost (ms)")
    private Long costTime;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;
}
