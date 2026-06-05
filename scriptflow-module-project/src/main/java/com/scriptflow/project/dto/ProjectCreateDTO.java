package com.scriptflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Project creation request DTO.
 */
@Data
@Schema(description = "Project creation request")
public class ProjectCreateDTO {

    @NotBlank(message = "Project name cannot be empty")
    @Schema(description = "Project name", example = "My Novel Script")
    private String name;

    @Schema(description = "Project description", example = "Convert my fantasy novel to script")
    private String description;

    @Schema(description = "Novel title", example = "The Dragon's Path")
    private String novelTitle;

    @Schema(description = "Novel author", example = "John Doe")
    private String author;

    @Schema(description = "Novel language", example = "zh")
    private String novelLanguage = "zh";

    @Schema(description = "Cover image URL")
    private String cover;
}
