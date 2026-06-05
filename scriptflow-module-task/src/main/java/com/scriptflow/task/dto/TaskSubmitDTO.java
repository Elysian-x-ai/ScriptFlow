package com.scriptflow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI task submission DTO.
 */
@Data
@Schema(description = "AI task submission request")
public class TaskSubmitDTO {

    @NotNull(message = "Project ID cannot be empty")
    @Schema(description = "Project ID")
    private Long projectId;

    @NotBlank(message = "Task type cannot be empty")
    @Schema(description = "Task type: novel_analysis, character_extract, script_generate, script_revise")
    private String taskType;

    @Schema(description = "Additional parameters (JSON)")
    private String params;
}
