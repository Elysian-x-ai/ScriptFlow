package com.scriptflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Login result with token.
 */
@Data
@AllArgsConstructor
@Schema(description = "Login result")
public class LoginResultVO {

    @Schema(description = "Access token")
    private String token;

    @Schema(description = "Token type")
    private String tokenType = "Bearer";

    @Schema(description = "User info")
    private UserVO user;

    public LoginResultVO(String token, UserVO user) {
        this.token = token;
        this.user = user;
    }
}
