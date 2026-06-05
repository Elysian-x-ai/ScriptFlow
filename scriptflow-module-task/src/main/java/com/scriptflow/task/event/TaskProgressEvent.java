package com.scriptflow.task.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TaskProgressEvent extends ApplicationEvent {

    private final Long taskId;
    private final Integer progress;
    private final Integer status;
    private final String result;
    private final String error;

    public TaskProgressEvent(Object source, Long taskId, Integer progress, Integer status, String result, String error) {
        super(source);
        this.taskId = taskId;
        this.progress = progress;
        this.status = status;
        this.result = result;
        this.error = error;
    }
}
