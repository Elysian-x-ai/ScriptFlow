package com.scriptflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Project update request DTO.
 */
@Data
@Schema(description = "Project update request")
public class ProjectUpdateDTO {

    @NotNull(message = "Project ID cannot be empty")
    @Schema(description = "Project ID")
    private Long id;

    @Schema(description = "Project name")
    private String name;

    @Schema(description = "Project description")
    private String description;

    @Schema(description = "Novel title")
    private String novelTitle;

    @Schema(description = "Novel author")
    private String author;

    @Schema(description = "Cover image URL")
    private String cover;
}
