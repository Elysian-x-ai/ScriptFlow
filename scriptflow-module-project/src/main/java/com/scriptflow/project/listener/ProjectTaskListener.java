package com.scriptflow.project.listener;

import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.framework.event.TaskCompletedEvent;
import com.scriptflow.project.service.ScriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for task completion events and updates project-level data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectTaskListener {

    private final ScriptService scriptService;

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        if (!GlobalConstants.TaskType.SCRIPT_GENERATE.equals(event.getTaskType())) {
            return;
        }

        if (event.getStatus() == GlobalConstants.TaskStatus.COMPLETED && event.getYamlContent() != null) {
            log.info("Task {} completed, updating script for project {}", event.getTaskId(), event.getProjectId());
            try {
                scriptService.updateContentByProject(event.getProjectId(), event.getYamlContent());
                log.info("Script updated successfully for project {}", event.getProjectId());
            } catch (Exception e) {
                log.error("Failed to update script for project {}: {}", event.getProjectId(), e.getMessage());
            }
        } else if (event.getStatus() == GlobalConstants.TaskStatus.FAILED) {
            log.warn("Script generation failed for project {}: {}", event.getProjectId(), event.getError());
        }
    }
}
