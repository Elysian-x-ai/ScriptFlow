package com.scriptflow.project.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.dal.entity.project.CharacterEntity;
import com.scriptflow.dal.entity.project.NovelChapter;
import com.scriptflow.dal.entity.project.Project;
import com.scriptflow.dal.entity.task.Task;
import com.scriptflow.dal.mapper.project.CharacterMapper;
import com.scriptflow.dal.mapper.project.NovelChapterMapper;
import com.scriptflow.dal.mapper.project.ProjectMapper;
import com.scriptflow.dal.mapper.task.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final CharacterMapper characterMapper;
    private final NovelChapterMapper chapterMapper;

    public DashboardVO getStats() {
        long projectCount = projectMapper.selectCount(null);
        long projectMonthCount = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>()
                        .ge(Project::getCreateTime, LocalDateTime.now().minusDays(30)));

        long taskCount = taskMapper.selectCount(null);
        long taskCompletedCount = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, GlobalConstants.TaskStatus.COMPLETED));
        long taskProcessingCount = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, GlobalConstants.TaskStatus.PROCESSING));
        long taskPendingCount = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, GlobalConstants.TaskStatus.PENDING));

        long characterCount = characterMapper.selectCount(null);
        long chapterCount = chapterMapper.selectCount(null);

        return new DashboardVO(projectCount, projectMonthCount, taskCount, taskCompletedCount,
                taskProcessingCount, taskPendingCount, characterCount, chapterCount);
    }
}
