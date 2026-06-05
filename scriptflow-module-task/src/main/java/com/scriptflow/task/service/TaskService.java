package com.scriptflow.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.dal.entity.task.Task;
import com.scriptflow.dal.entity.task.TaskLog;
import com.scriptflow.dal.mapper.task.TaskLogMapper;
import com.scriptflow.dal.mapper.task.TaskMapper;
import com.scriptflow.task.dto.TaskLogVO;
import com.scriptflow.task.dto.TaskSubmitDTO;
import com.scriptflow.task.dto.TaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI task orchestration service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskLogMapper taskLogMapper;

    public PageUtils<TaskVO> page(int page, int pageSize, Long projectId, String taskType) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(projectId != null, Task::getProjectId, projectId)
                .eq(taskType != null, Task::getTaskType, taskType)
                .orderByDesc(Task::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Task> mpPage =
                taskMapper.selectPage(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize),
                        wrapper);

        List<TaskVO> records = mpPage.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageUtils.of((int) mpPage.getCurrent(), (int) mpPage.getSize(),
                (int) mpPage.getTotal(), records);
    }

    public TaskVO getById(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        return toVO(task);
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

        // In a real implementation, this would send a message to RabbitMQ
        // to trigger the Python AI microservice asynchronously.

        return toVO(task);
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

    /**
     * Update task progress (called by async worker).
     */
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
    }

    // ---- Task Log ----

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

    private TaskVO toVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setProjectId(task.getProjectId());
        vo.setTaskType(task.getTaskType());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setStartTime(task.getStartTime());
        vo.setFinishTime(task.getFinishTime());
        vo.setCreateTime(task.getCreateTime());
        return vo;
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
