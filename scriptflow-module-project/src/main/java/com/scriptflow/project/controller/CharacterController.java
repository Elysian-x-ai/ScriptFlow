package com.scriptflow.project.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.project.dto.CharacterCreateDTO;
import com.scriptflow.project.dto.CharacterVO;
import com.scriptflow.project.service.CharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Character management controller.
 */
@Tag(name = "Character Management", description = "Character CRUD for novel projects")
@RestController
@RequestMapping("/api/project/character")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @Operation(summary = "List characters by project")
    @GetMapping("/list/{projectId}")
    public R<List<CharacterVO>> listByProject(@PathVariable Long projectId) {
        return R.success(characterService.listByProjectId(projectId));
    }

    @Operation(summary = "Get character by ID")
    @GetMapping("/{id}")
    public R<CharacterVO> getById(@PathVariable Long id) {
        return R.success(characterService.getById(id));
    }

    @Operation(summary = "Create character")
    @PostMapping
    public R<CharacterVO> create(@Valid @RequestBody CharacterCreateDTO dto) {
        return R.success(characterService.create(dto));
    }

    @Operation(summary = "Update character")
    @PutMapping("/{id}")
    public R<CharacterVO> update(@PathVariable Long id, @Valid @RequestBody CharacterCreateDTO dto) {
        return R.success(characterService.update(id, dto));
    }

    @Operation(summary = "Delete character")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        characterService.delete(id);
        return R.success();
    }
}
