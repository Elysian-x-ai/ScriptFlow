package com.scriptflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Registration request DTO.
 */
@Data
@Schema(description = "Registration request")
public class RegisterDTO {

    @NotBlank(message = "Username cannot be empty")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,30}$", message = "Username must be 4-30 alphanumeric characters")
    @Schema(description = "Username", example = "newuser")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    @Pattern(regexp = "^.{6,50}$", message = "Password must be 6-50 characters")
    @Schema(description = "Password", example = "password123")
    private String password;

    @Schema(description = "Display name", example = "New User")
    private String nickname;

    @Email(message = "Invalid email format")
    @Schema(description = "Email", example = "user@example.com")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number")
    @Schema(description = "Phone number", example = "13800138000")
    private String phone;
}
