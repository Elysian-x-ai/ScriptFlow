package com.scriptflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User view object returned to client.
 */
@Data
@Schema(description = "User view object")
public class UserVO {

    @Schema(description = "User ID")
    private Long id;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Display name")
    private String nickname;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Phone")
    private String phone;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Status: 1=enabled, 0=disabled")
    private Integer status;

    @Schema(description = "Role codes")
    private List<String> roles;

    @Schema(description = "Permission codes")
    private List<String> permissions;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;
}
