package com.scriptflow.system.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.dal.entity.system.SysRole;
import com.scriptflow.system.dto.RoleDTO;
import com.scriptflow.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Role management controller.
 */
@Tag(name = "Role Management", description = "Role CRUD and permission assignment")
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Paginated role list")
    @GetMapping("/page")
    public R<PageUtils<SysRole>> page(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return R.success(roleService.page(page, pageSize));
    }

    @Operation(summary = "List all roles")
    @GetMapping("/list")
    public R<List<SysRole>> listAll() {
        return R.success(roleService.listAll());
    }

    @Operation(summary = "Create role")
    @PostMapping
    public R<SysRole> create(@Valid @RequestBody RoleDTO dto) {
        return R.success(roleService.create(dto));
    }

    @Operation(summary = "Update role")
    @PutMapping
    public R<SysRole> update(@Valid @RequestBody RoleDTO dto) {
        return R.success(roleService.update(dto));
    }

    @Operation(summary = "Delete role")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable("id") Long id) {
        roleService.delete(id);
        return R.success();
    }
}
