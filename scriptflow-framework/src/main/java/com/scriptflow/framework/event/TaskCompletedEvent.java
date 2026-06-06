package com.scriptflow.framework.event;

import lombok.Getter;

@Getter
public class TaskCompletedEvent {

    private final Long taskId;
    private final Long projectId;
    private final Long scriptId;
    private final String taskType;
    private final Integer status;
    private final String yamlContent;
    private final String error;

    public TaskCompletedEvent(Long taskId, Long projectId, Long scriptId, String taskType, Integer status, String yamlContent, String error) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.scriptId = scriptId;
        this.taskType = taskType;
        this.status = status;
        this.yamlContent = yamlContent;
        this.error = error;
    }
}
