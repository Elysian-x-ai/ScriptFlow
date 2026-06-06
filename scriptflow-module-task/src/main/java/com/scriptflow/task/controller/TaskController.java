package com.scriptflow.task.controller;

import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.common.result.R;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.task.dto.TaskLogVO;
import com.scriptflow.task.dto.TaskSubmitDTO;
import com.scriptflow.task.dto.TaskVO;
import com.scriptflow.task.event.TaskProgressEvent;
import com.scriptflow.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI task management controller.
 */
@Tag(name = "Task Management", description = "AI task submission, monitoring, SSE progress, and cancellation")
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    private final Map<Long, List<SseEmitter>> taskEmitters = new ConcurrentHashMap<>();

    @Operation(summary = "Subscribe to task progress via SSE")
    @GetMapping("/{id}/stream")
    public SseEmitter streamProgress(@PathVariable("id") Long id) {
        SseEmitter emitter = new SseEmitter(0L);
        taskEmitters.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(id, emitter));
        emitter.onTimeout(() -> removeEmitter(id, emitter));
        emitter.onError(e -> removeEmitter(id, emitter));

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"taskId\":" + id + "}"));
        } catch (IOException e) {
            removeEmitter(id, emitter);
        }
        return emitter;
    }

    @EventListener
    public void handleTaskProgress(TaskProgressEvent event) {
        List<SseEmitter> emitters = taskEmitters.get(event.getTaskId());
        if (emitters == null || emitters.isEmpty()) return;

        StringBuilder json = new StringBuilder();
        json.append("{\"taskId\":").append(event.getTaskId())
                .append(",\"progress\":").append(event.getProgress())
                .append(",\"status\":").append(event.getStatus());
        if (event.getResult() != null) json.append(",\"result\":\"").append(escape(event.getResult())).append("\"");
        if (event.getError() != null) json.append(",\"error\":\"").append(escape(event.getError())).append("\"");
        json.append("}");

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(json.toString()));
                if (event.getStatus() != null && (event.getStatus() == GlobalConstants.TaskStatus.COMPLETED || event.getStatus() == GlobalConstants.TaskStatus.FAILED || event.getStatus() == GlobalConstants.TaskStatus.CANCELLED)) {
                    emitter.complete();
                }
            } catch (IOException e) {
                removeEmitter(event.getTaskId(), emitter);
            }
        }
    }

    private void removeEmitter(Long taskId, SseEmitter emitter) {
        List<SseEmitter> emitters = taskEmitters.get(taskId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                taskEmitters.remove(taskId);
            }
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @Operation(summary = "Submit AI processing task")
    @PostMapping("/submit")
    public R<TaskVO> submit(@Valid @RequestBody TaskSubmitDTO dto,
                            @RequestParam(value = "userId", defaultValue = "0") Long userId) {
        return R.success(taskService.submit(dto, userId));
    }

    @Operation(summary = "Get task by ID")
    @GetMapping("/{id}")
    public R<TaskVO> getById(@PathVariable("id") Long id) {
        return R.success(taskService.getById(id));
    }

    @Operation(summary = "Paginated task list")
    @GetMapping("/page")
    public R<PageUtils<TaskVO>> page(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "taskType", required = false) String taskType) {
        return R.success(taskService.page(page, pageSize, projectId, taskType));
    }

    @Operation(summary = "Cancel task")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable("id") Long id) {
        taskService.cancel(id);
        return R.success();
    }

    @Operation(summary = "List task logs")
    @GetMapping("/{id}/logs")
    public R<List<TaskLogVO>> listLogs(@PathVariable("id") Long id) {
        return R.success(taskService.listLogs(id));
    }
}
