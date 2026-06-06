package com.scriptflow.project.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.project.dto.ScriptVO;
import com.scriptflow.project.dto.ValidationResult;
import com.scriptflow.project.service.ScriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Script management controller.
 */
@Tag(name = "Script Management", description = "Script CRUD, versioning, generation, and YAML validation")
@RestController
@RequestMapping("/api/project/script")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;

    @Operation(summary = "Get script by ID")
    @GetMapping("/{id}")
    public R<ScriptVO> getById(@PathVariable("id") Long id) {
        return R.success(scriptService.getById(id));
    }

    @Operation(summary = "Get script by project")
    @GetMapping("/project/{projectId}")
    public R<ScriptVO> getByProject(@PathVariable("projectId") Long projectId) {
        return R.success(scriptService.getByProjectId(projectId));
    }

    @Operation(summary = "Submit script generation task")
    @PostMapping("/generate/{projectId}")
    public R<ScriptVO> submitGeneration(@PathVariable("projectId") Long projectId,
                                        @RequestParam(value = "userId", defaultValue = "0") Long userId) {
        return R.success(scriptService.submitGeneration(projectId, userId));
    }

    @Operation(summary = "Validate YAML script content")
    @PostMapping("/validate")
    public R<ValidationResult> validate(@RequestBody String yamlContent) {
        List<String> errors = new ArrayList<>();

        if (yamlContent == null || yamlContent.isBlank()) {
            return R.success(ValidationResult.failed(List.of("YAML content is empty")));
        }

        if (!yamlContent.contains("meta:") && !yamlContent.contains("meta :")) {
            errors.add("Missing required section: 'meta'");
        }
        if (!yamlContent.contains("characters:") && !yamlContent.contains("characters :")) {
            errors.add("Missing required section: 'characters'");
        }
        if (!yamlContent.contains("acts:") && !yamlContent.contains("acts :")) {
            errors.add("Missing required section: 'acts'");
        }

        // Check YAML indentation consistency
        String[] lines = yamlContent.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("\t")) {
                errors.add("Line " + (i + 1) + ": contains tabs, use spaces for indentation");
            }
        }

        if (errors.isEmpty()) {
            return R.success(ValidationResult.ok());
        }
        return R.success(ValidationResult.failed(errors));
    }

    @Operation(summary = "List all versions of a script")
    @GetMapping("/version/list/{scriptId}")
    public R<List<ScriptVO>> listVersions(@PathVariable("scriptId") Long scriptId) {
        return R.success(scriptService.listVersions(scriptId));
    }

    @Operation(summary = "Create new version")
    @PostMapping("/version/{scriptId}")
    public R<ScriptVO> createVersion(@PathVariable("scriptId") Long scriptId,
                                     @RequestBody Map<String, Object> params) {
        Object yamlRaw = params.get("yamlContent");
        Object logRaw = params.get("changeLog");
        String yamlContent = yamlRaw instanceof String ? (String) yamlRaw : null;
        String changeLog = logRaw instanceof String ? (String) logRaw : null;
        return R.success(scriptService.createVersion(scriptId, yamlContent, changeLog));
    }
}
