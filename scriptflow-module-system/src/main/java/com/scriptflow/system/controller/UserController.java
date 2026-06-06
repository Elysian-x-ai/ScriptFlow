package com.scriptflow.system.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.system.dto.UserVO;
import com.scriptflow.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * User management controller (admin).
 */
@Tag(name = "User Management", description = "User CRUD and management")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Paginated user list")
    @GetMapping("/page")
    public R<PageUtils<UserVO>> page(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return R.success(userService.page(page, pageSize, keyword));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public R<UserVO> getById(@PathVariable("id") Long id) {
        return R.success(userService.getById(id));
    }

    @Operation(summary = "Update user status")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        userService.updateStatus(id, status);
        return R.success();
    }
}
