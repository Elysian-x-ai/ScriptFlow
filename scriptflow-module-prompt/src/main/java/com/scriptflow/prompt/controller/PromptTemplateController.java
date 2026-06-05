package com.scriptflow.prompt.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.dal.entity.prompt.PromptTemplate;
import com.scriptflow.prompt.dto.PromptTemplateDTO;
import com.scriptflow.prompt.service.PromptTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prompt template management controller.
 */
@Tag(name = "Prompt Template", description = "Prompt template management for AI configuration")
@RestController
@RequestMapping("/api/prompt")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    @Operation(summary = "Paginated template list")
    @GetMapping("/page")
    public R<PageUtils<PromptTemplate>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        return R.success(promptTemplateService.page(page, pageSize, category, type));
    }

    @Operation(summary = "List templates by category")
    @GetMapping("/list/{category}")
    public R<List<PromptTemplate>> listByCategory(@PathVariable String category) {
        return R.success(promptTemplateService.listByCategory(category));
    }

    @Operation(summary = "Get template by ID")
    @GetMapping("/{id}")
    public R<PromptTemplate> getById(@PathVariable Long id) {
        return R.success(promptTemplateService.getById(id));
    }

    @Operation(summary = "Get template by code")
    @GetMapping("/code/{code}")
    public R<PromptTemplate> getByCode(@PathVariable String code) {
        return R.success(promptTemplateService.getByCode(code));
    }

    @Operation(summary = "Create template")
    @PostMapping
    public R<PromptTemplate> create(@Valid @RequestBody PromptTemplateDTO dto) {
        return R.success(promptTemplateService.create(dto));
    }

    @Operation(summary = "Update template")
    @PutMapping
    public R<PromptTemplate> update(@Valid @RequestBody PromptTemplateDTO dto) {
        return R.success(promptTemplateService.update(dto));
    }

    @Operation(summary = "Delete template")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        promptTemplateService.delete(id);
        return R.success();
    }
}
