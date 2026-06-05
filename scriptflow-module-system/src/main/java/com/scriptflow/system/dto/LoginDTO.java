package com.scriptflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login request DTO.
 */
@Data
@Schema(description = "Login request")
public class LoginDTO {

    @NotBlank(message = "Username cannot be empty")
    @Schema(description = "Username", example = "admin")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    @Schema(description = "Password", example = "123456")
    private String password;
}
