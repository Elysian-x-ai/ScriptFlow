package com.scriptflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.BeanCopyUtils;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.common.util.StringUtils;
import com.scriptflow.dal.entity.project.Project;
import com.scriptflow.dal.mapper.project.ProjectMapper;
import com.scriptflow.framework.service.BaseService;
import com.scriptflow.framework.service.Converter;
import com.scriptflow.project.dto.ProjectCreateDTO;
import com.scriptflow.project.dto.ProjectUpdateDTO;
import com.scriptflow.project.dto.ProjectVO;
import com.scriptflow.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService extends BaseService<Project, ProjectVO> {

    private final ProjectMapper projectMapper;
    private final FileStorageService fileStorageService;

    @Override
    protected BaseMapper<Project> getMapper() {
        return projectMapper;
    }

    @Override
    protected Converter<Project, ProjectVO> getConverter() {
        return entity -> BeanCopyUtils.copy(entity, ProjectVO.class);
    }

    @Override
    public ProjectVO getById(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null || project.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Project not found");
        }
        return getConverter().convert(project);
    }

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
        return super.page(page, pageSize, wrapper);
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
        project.setStatus(GlobalConstants.ProjectStatus.ACTIVE);
        project.setUserId(userId);
        project.setChapterCount(0);
        projectMapper.insert(project);
        return getConverter().convert(project);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectVO update(ProjectUpdateDTO dto) {
        Project project = findByIdOrThrow(dto.getId());
        if (StringUtils.isNotBlank(dto.getName())) project.setName(dto.getName());
        if (StringUtils.isNotBlank(dto.getDescription())) project.setDescription(dto.getDescription());
        if (StringUtils.isNotBlank(dto.getNovelTitle())) project.setNovelTitle(dto.getNovelTitle());
        if (StringUtils.isNotBlank(dto.getAuthor())) project.setAuthor(dto.getAuthor());
        if (StringUtils.isNotBlank(dto.getCover())) project.setCover(dto.getCover());
        projectMapper.updateById(project);
        return getConverter().convert(project);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        findByIdOrThrow(id);

        // Clean up MinIO objects for this project (chapter JSON files)
        try {
            fileStorageService.removeByPrefix("novel-content/" + id + "/");
        } catch (Exception e) {
            log.warn("Failed to clean up MinIO objects for project {}: {}", id, e.getMessage());
        }

        projectMapper.deleteById(id);
        log.info("Project deleted: {}", id);
    }
}
