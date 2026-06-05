package com.scriptflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Chapter update DTO.
 */
@Data
@Schema(description = "Chapter update request")
public class ChapterUpdateDTO {

    @NotNull(message = "Chapter ID cannot be empty")
    @Schema(description = "Chapter ID")
    private Long id;

    @Schema(description = "Chapter title")
    private String title;

    @Schema(description = "Chapter content")
    private String content;

    @Schema(description = "Chapter number")
    private Integer chapterNo;
}
