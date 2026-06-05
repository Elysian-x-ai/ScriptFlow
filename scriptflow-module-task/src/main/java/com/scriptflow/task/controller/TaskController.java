package com.scriptflow.task.controller;

import com.scriptflow.common.result.R;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.task.dto.TaskLogVO;
import com.scriptflow.task.dto.TaskSubmitDTO;
import com.scriptflow.task.dto.TaskVO;
import com.scriptflow.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI task management controller.
 */
@Tag(name = "Task Management", description = "AI task submission, monitoring, and cancellation")
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Submit AI processing task")
    @PostMapping("/submit")
    public R<TaskVO> submit(@Valid @RequestBody TaskSubmitDTO dto,
                            @RequestParam(defaultValue = "0") Long userId) {
        return R.success(taskService.submit(dto, userId));
    }

    @Operation(summary = "Get task by ID")
    @GetMapping("/{id}")
    public R<TaskVO> getById(@PathVariable Long id) {
        return R.success(taskService.getById(id));
    }

    @Operation(summary = "Paginated task list")
    @GetMapping("/page")
    public R<PageUtils<TaskVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String taskType) {
        return R.success(taskService.page(page, pageSize, projectId, taskType));
    }

    @Operation(summary = "Cancel task")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        taskService.cancel(id);
        return R.success();
    }

    @Operation(summary = "List task logs")
    @GetMapping("/{id}/logs")
    public R<List<TaskLogVO>> listLogs(@PathVariable Long id) {
        return R.success(taskService.listLogs(id));
    }
}
