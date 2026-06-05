package com.scriptflow.dal.entity.task;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI processing task entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pro_task")
public class Task extends BaseEntity {

    private Long projectId;
    private String taskType;
    private Integer status;
    private Integer progress;
    private String requestParams;
    private String resultData;
    private String errorMsg;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private Long createBy;
}
