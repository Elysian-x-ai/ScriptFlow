package com.scriptflow.project.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.project.dto.ProjectCreateDTO;
import com.scriptflow.project.dto.ProjectUpdateDTO;
import com.scriptflow.project.dto.ProjectVO;
import com.scriptflow.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Project management controller.
 */
@Tag(name = "Project Management", description = "Project CRUD and query")
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Paginated project list")
    @GetMapping("/page")
    public R<PageUtils<ProjectVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword) {
        return R.success(projectService.page(page, pageSize, userId, keyword));
    }

    @Operation(summary = "Get project by ID")
    @GetMapping("/{id}")
    public R<ProjectVO> getById(@PathVariable Long id) {
        return R.success(projectService.getById(id));
    }

    @Operation(summary = "Create project")
    @PostMapping
    public R<ProjectVO> create(@Valid @RequestBody ProjectCreateDTO dto,
                               @RequestParam(defaultValue = "0") Long userId) {
        return R.success(projectService.create(dto, userId));
    }

    @Operation(summary = "Update project")
    @PutMapping
    public R<ProjectVO> update(@Valid @RequestBody ProjectUpdateDTO dto) {
        return R.success(projectService.update(dto));
    }

    @Operation(summary = "Delete project")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return R.success();
    }
}
