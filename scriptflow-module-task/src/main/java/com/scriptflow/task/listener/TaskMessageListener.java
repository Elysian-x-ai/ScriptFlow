package com.scriptflow.task.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scriptflow.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RabbitMQ listener for AI task processing results.
 * Receives async completion messages from the Python AI microservice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageListener {

    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${scriptflow.rabbit.queue.result:scriptflow.task.result}")
    public void handleTaskResult(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            Long taskId = Long.valueOf(data.get("taskId").toString());
            Integer status = (Integer) data.get("status");
            String result = (String) data.get("result");
            String error = (String) data.get("error");
            Integer progress = data.containsKey("progress") ? (Integer) data.get("progress") : 100;

            taskService.updateProgress(taskId, progress, status, result, error);
            log.info("Task {} updated to status {} with progress {}", taskId, status, progress);
        } catch (Exception e) {
            log.error("Failed to process task result message: {}", message, e);
        }
    }

    @RabbitListener(queues = "${scriptflow.rabbit.queue.log:scriptflow.task.log}")
    public void handleTaskLog(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            Long taskId = Long.valueOf(data.get("taskId").toString());
            String stage = (String) data.get("stage");
            Integer status = (Integer) data.get("status");
            String logMessage = (String) data.get("message");
            Long costTime = data.containsKey("costTime") ? Long.valueOf(data.get("costTime").toString()) : 0L;

            taskService.addLog(taskId, stage, status, logMessage, costTime);
        } catch (Exception e) {
            log.error("Failed to process task log message: {}", message, e);
        }
    }
}
