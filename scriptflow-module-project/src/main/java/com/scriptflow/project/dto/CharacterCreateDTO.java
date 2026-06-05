package com.scriptflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Character creation DTO.
 */
@Data
@Schema(description = "Character creation request")
public class CharacterCreateDTO {

    @NotNull(message = "Project ID cannot be empty")
    @Schema(description = "Project ID")
    private Long projectId;

    @NotBlank(message = "Character name cannot be empty")
    @Schema(description = "Character name", example = "John Smith")
    private String name;

    @Schema(description = "Alias", example = "The Dragon Knight")
    private String alias;

    @Schema(description = "Gender", example = "male")
    private String gender;

    @Schema(description = "Age", example = "28")
    private String age;

    @Schema(description = "Personality", example = "Brave, loyal, impulsive")
    private String personality;

    @Schema(description = "Appearance")
    private String appearance;

    @Schema(description = "Background")
    private String background;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Role type", example = "protagonist")
    private String roleType;
}
