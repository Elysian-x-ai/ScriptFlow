package com.scriptflow.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.BeanCopyUtils;
import com.scriptflow.common.util.JsonUtils;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.dal.entity.task.Task;
import com.scriptflow.dal.entity.task.TaskLog;
import com.scriptflow.dal.mapper.task.TaskLogMapper;
import com.scriptflow.dal.mapper.task.TaskMapper;
import com.scriptflow.framework.service.BaseService;
import com.scriptflow.framework.service.Converter;
import com.scriptflow.task.dto.TaskLogVO;
import com.scriptflow.task.dto.TaskSubmitDTO;
import com.scriptflow.task.dto.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scriptflow.task.event.TaskProgressEvent;
import com.scriptflow.framework.event.TaskCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService extends BaseService<Task, TaskVO> {

    private final TaskMapper taskMapper;
    private final TaskLogMapper taskLogMapper;
    private final RabbitTemplate rabbitTemplate;
    private final JsonUtils jsonUtils;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${scriptflow.rabbit.queue.task:scriptflow.task.submit}")
    private String taskQueue;

    @Override
    protected BaseMapper<Task> getMapper() {
        return taskMapper;
    }

    @Override
    protected Converter<Task, TaskVO> getConverter() {
        return entity -> BeanCopyUtils.copy(entity, TaskVO.class);
    }

    @Override
    public TaskVO getById(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        return getConverter().convert(task);
    }

    public PageUtils<TaskVO> page(int page, int pageSize, Long projectId, String taskType) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(projectId != null, Task::getProjectId, projectId)
                .eq(taskType != null, Task::getTaskType, taskType)
                .orderByDesc(Task::getCreateTime);
        return super.page(page, pageSize, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskVO submit(TaskSubmitDTO dto, Long userId) {
        Task task = new Task();
        task.setProjectId(dto.getProjectId());
        task.setTaskType(dto.getTaskType());
        task.setStatus(GlobalConstants.TaskStatus.PENDING);
        task.setProgress(0);
        task.setRequestParams(dto.getParams());
        task.setCreateBy(userId);
        taskMapper.insert(task);

        log.info("Task submitted: type={}, projectId={}, taskId={}",
                dto.getTaskType(), dto.getProjectId(), task.getId());

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("taskId", task.getId());
            message.put("taskType", dto.getTaskType());
            message.put("projectId", dto.getProjectId());
            message.put("params", dto.getParams());
            message.put("userId", userId);
            rabbitTemplate.convertAndSend(taskQueue, jsonUtils.toJson(message));
            log.info("Task published to RabbitMQ queue '{}': taskId={}", taskQueue, task.getId());
        } catch (Exception e) {
            log.warn("Failed to publish task to RabbitMQ, task will be processed later: taskId={}", task.getId(), e);
        }

        return getConverter().convert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        if (task.getStatus() == GlobalConstants.TaskStatus.COMPLETED) {
            throw new BusinessException(ResultCode.TASK_PROCESSING, "Task already completed");
        }
        task.setStatus(GlobalConstants.TaskStatus.CANCELLED);
        taskMapper.updateById(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long taskId, int progress, Integer status, String result, String error) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;
        task.setProgress(progress);
        if (status != null) {
            task.setStatus(status);
            if (status == GlobalConstants.TaskStatus.PROCESSING) {
                task.setStartTime(LocalDateTime.now());
            } else if (status == GlobalConstants.TaskStatus.COMPLETED) {
                task.setFinishTime(LocalDateTime.now());
                task.setResultData(result);
            } else if (status == GlobalConstants.TaskStatus.FAILED) {
                task.setFinishTime(LocalDateTime.now());
                task.setErrorMsg(error);
            }
        }
        taskMapper.updateById(task);

        eventPublisher.publishEvent(new TaskProgressEvent(this, taskId, progress, status, result, error));

        // Publish cross-module event for task completion
        if (status != null && (status == GlobalConstants.TaskStatus.COMPLETED || status == GlobalConstants.TaskStatus.FAILED)) {
            eventPublisher.publishEvent(new TaskCompletedEvent(
                    taskId, task.getProjectId(), task.getTaskType(), status, result, error));
        }
    }

    public List<TaskLogVO> listLogs(Long taskId) {
        return taskLogMapper.selectList(
                        new LambdaQueryWrapper<TaskLog>()
                                .eq(TaskLog::getTaskId, taskId)
                                .orderByAsc(TaskLog::getCreateTime))
                .stream()
                .map(this::toLogVO)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void addLog(Long taskId, String stage, Integer status, String message, Long costTime) {
        TaskLog logEntry = new TaskLog();
        logEntry.setTaskId(taskId);
        logEntry.setStage(stage);
        logEntry.setStatus(status);
        logEntry.setMessage(message);
        logEntry.setCostTime(costTime);
        taskLogMapper.insert(logEntry);
    }

    private TaskLogVO toLogVO(TaskLog logEntry) {
        TaskLogVO vo = new TaskLogVO();
        vo.setId(logEntry.getId());
        vo.setTaskId(logEntry.getTaskId());
        vo.setStage(logEntry.getStage());
        vo.setStatus(logEntry.getStatus());
        vo.setMessage(logEntry.getMessage());
        vo.setCostTime(logEntry.getCostTime());
        vo.setCreateTime(logEntry.getCreateTime());
        return vo;
    }
}
