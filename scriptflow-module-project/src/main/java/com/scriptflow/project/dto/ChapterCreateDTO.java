package com.scriptflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Novel chapter creation DTO.
 */
@Data
@Schema(description = "Chapter creation request")
public class ChapterCreateDTO {

    @NotNull(message = "Project ID cannot be empty")
    @Schema(description = "Project ID")
    private Long projectId;

    @NotNull(message = "Chapter number cannot be empty")
    @Schema(description = "Chapter sequence number", example = "1")
    private Integer chapterNo;

    @Schema(description = "Chapter title", example = "Chapter 1: The Beginning")
    private String title;

    @NotBlank(message = "Content cannot be empty")
    @Schema(description = "Chapter content")
    private String content;
}
