package com.scriptflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Character view object.
 */
@Data
@Schema(description = "Character view object")
public class CharacterVO {

    @Schema(description = "Character ID")
    private Long id;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Character name")
    private String name;

    @Schema(description = "Alias")
    private String alias;

    @Schema(description = "Gender")
    private String gender;

    @Schema(description = "Age")
    private String age;

    @Schema(description = "Personality")
    private String personality;

    @Schema(description = "Appearance")
    private String appearance;

    @Schema(description = "Background")
    private String background;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Role type")
    private String roleType;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;
}
