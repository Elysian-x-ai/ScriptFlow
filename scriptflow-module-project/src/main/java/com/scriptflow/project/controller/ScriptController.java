package com.scriptflow.project.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.project.dto.ScriptVO;
import com.scriptflow.project.service.ScriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Script management controller.
 */
@Tag(name = "Script Management", description = "Script CRUD, versioning, and generation")
@RestController
@RequestMapping("/api/project/script")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;

    @Operation(summary = "Get script by ID")
    @GetMapping("/{id}")
    public R<ScriptVO> getById(@PathVariable Long id) {
        return R.success(scriptService.getById(id));
    }

    @Operation(summary = "Get script by project")
    @GetMapping("/project/{projectId}")
    public R<ScriptVO> getByProject(@PathVariable Long projectId) {
        return R.success(scriptService.getByProjectId(projectId));
    }

    @Operation(summary = "Submit script generation task")
    @PostMapping("/generate/{projectId}")
    public R<ScriptVO> submitGeneration(@PathVariable Long projectId,
                                        @RequestParam(defaultValue = "0") Long userId) {
        return R.success(scriptService.submitGeneration(projectId, userId));
    }

    @Operation(summary = "List all versions of a script")
    @GetMapping("/version/list/{scriptId}")
    public R<List<ScriptVO>> listVersions(@PathVariable Long scriptId) {
        return R.success(scriptService.listVersions(scriptId));
    }

    @Operation(summary = "Create new version")
    @PostMapping("/version/{scriptId}")
    public R<ScriptVO> createVersion(@PathVariable Long scriptId,
                                     @RequestParam String yamlContent,
                                     @RequestParam(required = false) String changeLog) {
        return R.success(scriptService.createVersion(scriptId, yamlContent, changeLog));
    }
}
