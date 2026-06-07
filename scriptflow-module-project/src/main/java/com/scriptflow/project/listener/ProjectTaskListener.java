package com.scriptflow.project.listener;

import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.dal.entity.project.Script;
import com.scriptflow.dal.mapper.project.ScriptMapper;
import com.scriptflow.framework.event.TaskCompletedEvent;
import com.scriptflow.project.service.ScriptService;
import com.scriptflow.storage.service.FileStorageService;
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
    private final ScriptMapper scriptMapper;
    private final FileStorageService fileStorageService;

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        if (!GlobalConstants.TaskType.SCRIPT_GENERATE.equals(event.getTaskType())) {
            return;
        }

        if (event.getStatus() == GlobalConstants.TaskStatus.COMPLETED) {
            if (event.getYamlContent() != null && !event.getYamlContent().isEmpty()) {
                Long scriptId = event.getScriptId();
                log.info("Task {} completed, updating script {} for project {}", event.getTaskId(), scriptId, event.getProjectId());
                try {
                    if (scriptId != null) {
                        scriptService.updateContent(scriptId, event.getYamlContent());
                        // Upload the generated YAML to MinIO for persistent storage
                        uploadScriptToMinio(event.getProjectId(), scriptId, event.getYamlContent());
                    } else {
                        // Fallback: update latest script for project (legacy path)
                        scriptService.updateContentByProject(event.getProjectId(), event.getYamlContent());
                    }
                    log.info("Script updated successfully for project {}", event.getProjectId());
                } catch (Exception e) {
                    log.error("Failed to update script for project {}: {}", event.getProjectId(), e.getMessage());
                }
            } else {
                log.warn("Task {} completed but YAML content is empty for project {}", event.getTaskId(), event.getProjectId());
            }
        } else if (event.getStatus() == GlobalConstants.TaskStatus.FAILED) {
            log.warn("Script generation failed for project {}: {}", event.getProjectId(), event.getError());
            // Mark Script as FAILED so it's not stuck in GENERATING status
            Long scriptId = event.getScriptId();
            if (scriptId != null) {
                try {
                    Script script = scriptMapper.selectById(scriptId);
                    if (script != null) {
                        script.setStatus(GlobalConstants.ScriptStatus.FAILED);
                        script.setErrorMsg(event.getError());
                        scriptMapper.updateById(script);
                    }
                } catch (Exception e) {
                    log.error("Failed to update script status on failure for {}", scriptId, e);
                }
            }
        }
    }

    /**
     * Upload the generated YAML script content to MinIO.
     * Key format: script-yaml/{projectId}/{scriptId}/v{version}_{timestamp}.yaml
     */
    private void uploadScriptToMinio(Long projectId, Long scriptId, String yamlContent) {
        try {
            Script script = scriptMapper.selectById(scriptId);
            if (script == null) {
                log.warn("Script {} not found, skipping MinIO upload", scriptId);
                return;
            }
            int version = script.getVersion() != null ? script.getVersion() : 1;
            String key = String.format("script-yaml/%d/%d/v%d_%d.yaml",
                    projectId, scriptId, version, System.currentTimeMillis());
            fileStorageService.uploadString(key, yamlContent);
            log.info("Script YAML uploaded to MinIO: {}", key);
        } catch (Exception e) {
            log.error("Failed to upload script YAML to MinIO for script {}: {}", scriptId, e.getMessage());
        }
    }
}
