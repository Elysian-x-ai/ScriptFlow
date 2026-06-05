package com.scriptflow.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Prompt template create/update DTO.
 */
@Data
@Schema(description = "Prompt template DTO")
public class PromptTemplateDTO {

    private Long id;

    @NotBlank(message = "Template name cannot be empty")
    @Schema(description = "Template name")
    private String name;

    @NotBlank(message = "Template code cannot be empty")
    @Schema(description = "Template code (unique)")
    private String code;

    @NotBlank(message = "Template type cannot be empty")
    @Schema(description = "Type: system/user")
    private String type;

    @Schema(description = "Category: novel_analysis/character_extract/script_generate")
    private String category;

    @NotBlank(message = "Content cannot be empty")
    @Schema(description = "Prompt content")
    private String content;

    @Schema(description = "Variable definitions (JSON)")
    private String variables;

    @Schema(description = "Template description")
    private String description;

    @Schema(description = "Status: 1=enabled, 0=disabled")
    private Integer status;
}
