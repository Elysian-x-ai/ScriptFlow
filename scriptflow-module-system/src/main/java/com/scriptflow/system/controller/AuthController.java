package com.scriptflow.system.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.system.dto.LoginDTO;
import com.scriptflow.system.dto.LoginResultVO;
import com.scriptflow.system.dto.RegisterDTO;
import com.scriptflow.system.dto.UserVO;
import com.scriptflow.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 */
@Tag(name = "Authentication", description = "Login, register, token management")
@RestController
@RequestMapping("/api/system/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User login")
    @PostMapping("/login")
    public R<LoginResultVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.success(authService.login(dto));
    }

    @Operation(summary = "User registration")
    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.success(authService.register(dto));
    }

    @Operation(summary = "Get current user info (from Sa-Token session)")
    @GetMapping("/info")
    public R<UserVO> info() {
        return R.success(authService.getCurrentUserInfo());
    }

    @Operation(summary = "User logout")
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.success();
    }
}
