package com.scriptflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.common.util.StringUtils;
import com.scriptflow.dal.entity.project.Project;
import com.scriptflow.dal.mapper.project.ProjectMapper;
import com.scriptflow.project.dto.ProjectCreateDTO;
import com.scriptflow.project.dto.ProjectUpdateDTO;
import com.scriptflow.project.dto.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Project management service.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;

    public PageUtils<ProjectVO> page(int page, int pageSize, Long userId, String keyword) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(userId != null, Project::getUserId, userId)
                .and(keyword != null, w -> w
                        .like(Project::getName, keyword)
                        .or()
                        .like(Project::getNovelTitle, keyword)
                        .or()
                        .like(Project::getAuthor, keyword))
                .orderByDesc(Project::getUpdateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Project> mpPage =
                projectMapper.selectPage(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize),
                        wrapper);

        List<ProjectVO> records = mpPage.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageUtils.of((int) mpPage.getCurrent(), (int) mpPage.getSize(),
                (int) mpPage.getTotal(), records);
    }

    public ProjectVO getById(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null || project.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Project not found");
        }
        return toVO(project);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectVO create(ProjectCreateDTO dto, Long userId) {
        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setNovelTitle(dto.getNovelTitle());
        project.setAuthor(dto.getAuthor());
        project.setNovelLanguage(StringUtils.isBlank(dto.getNovelLanguage()) ? "zh" : dto.getNovelLanguage());
        project.setCover(dto.getCover());
        project.setStatus(1);
        project.setUserId(userId);
        project.setChapterCount(0);
        projectMapper.insert(project);
        return toVO(project);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectVO update(ProjectUpdateDTO dto) {
        Project project = projectMapper.selectById(dto.getId());
        if (project == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Project not found");
        }
        if (StringUtils.isNotBlank(dto.getName())) project.setName(dto.getName());
        if (StringUtils.isNotBlank(dto.getDescription())) project.setDescription(dto.getDescription());
        if (StringUtils.isNotBlank(dto.getNovelTitle())) project.setNovelTitle(dto.getNovelTitle());
        if (StringUtils.isNotBlank(dto.getAuthor())) project.setAuthor(dto.getAuthor());
        if (StringUtils.isNotBlank(dto.getCover())) project.setCover(dto.getCover());
        projectMapper.updateById(project);
        return toVO(project);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Project not found");
        }
        projectMapper.deleteById(id);
    }

    private ProjectVO toVO(Project project) {
        ProjectVO vo = new ProjectVO();
        vo.setId(project.getId());
        vo.setName(project.getName());
        vo.setDescription(project.getDescription());
        vo.setCover(project.getCover());
        vo.setNovelTitle(project.getNovelTitle());
        vo.setAuthor(project.getAuthor());
        vo.setNovelLanguage(project.getNovelLanguage());
        vo.setChapterCount(project.getChapterCount());
        vo.setStatus(project.getStatus());
        vo.setUserId(project.getUserId());
        vo.setCreateTime(project.getCreateTime());
        vo.setUpdateTime(project.getUpdateTime());
        return vo;
    }
}
