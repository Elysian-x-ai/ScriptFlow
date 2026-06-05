package com.scriptflow.dal.entity.task;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI task execution log entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pro_task_log")
public class TaskLog extends BaseEntity {

    private Long taskId;
    private String stage;
    private Integer status;
    private String message;
    private Long costTime;
}
