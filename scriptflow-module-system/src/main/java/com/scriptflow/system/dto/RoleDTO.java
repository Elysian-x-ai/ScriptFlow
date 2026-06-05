package com.scriptflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Role create/update DTO.
 */
@Data
@Schema(description = "Role DTO")
public class RoleDTO {

    @Schema(description = "Role ID (null for create)")
    private Long id;

    @NotBlank(message = "Role name cannot be empty")
    @Schema(description = "Role name", example = "Administrator")
    private String name;

    @NotBlank(message = "Role code cannot be empty")
    @Pattern(regexp = "^[A-Z_]+$", message = "Role code must be uppercase letters and underscores")
    @Schema(description = "Role code", example = "ADMIN")
    private String code;

    @Schema(description = "Role description", example = "System administrator")
    private String description;

    @Schema(description = "Status: 1=enabled, 0=disabled", example = "1")
    private Integer status;

    @Schema(description = "Permission ID list")
    private java.util.List<Long> permissionIds;
}
