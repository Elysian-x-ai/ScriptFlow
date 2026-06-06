package com.scriptflow.project.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.project.dto.ChapterCreateDTO;
import com.scriptflow.project.dto.ChapterUpdateDTO;
import com.scriptflow.project.dto.ChapterVO;
import com.scriptflow.project.service.NovelChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Novel chapter management controller.
 */
@Tag(name = "Novel Chapter", description = "Chapter CRUD for novel content")
@RestController
@RequestMapping("/api/project/novel/chapter")
@RequiredArgsConstructor
public class NovelChapterController {

    private final NovelChapterService chapterService;

    @Operation(summary = "List chapters by project")
    @GetMapping("/list/{projectId}")
    public R<List<ChapterVO>> listByProject(@PathVariable("projectId") Long projectId) {
        return R.success(chapterService.listByProjectId(projectId));
    }

    @Operation(summary = "Get chapter by ID")
    @GetMapping("/{id}")
    public R<ChapterVO> getById(@PathVariable("id") Long id) {
        return R.success(chapterService.getById(id));
    }

    @Operation(summary = "Create chapter")
    @PostMapping
    public R<ChapterVO> create(@Valid @RequestBody ChapterCreateDTO dto) {
        return R.success(chapterService.create(dto));
    }

    @Operation(summary = "Update chapter")
    @PutMapping
    public R<ChapterVO> update(@Valid @RequestBody ChapterUpdateDTO dto) {
        return R.success(chapterService.update(dto));
    }

    @Operation(summary = "Delete chapter")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable("id") Long id) {
        chapterService.delete(id);
        return R.success();
    }
}
