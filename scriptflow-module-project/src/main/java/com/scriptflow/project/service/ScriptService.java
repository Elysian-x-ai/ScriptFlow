package com.scriptflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.JsonUtils;
import com.scriptflow.storage.service.FileStorageService;
import com.scriptflow.dal.entity.project.NovelChapter;
import com.scriptflow.dal.entity.project.Script;
import com.scriptflow.dal.entity.project.ScriptVersion;
import com.scriptflow.dal.mapper.project.NovelChapterMapper;
import com.scriptflow.dal.mapper.project.ScriptMapper;
import com.scriptflow.dal.mapper.project.ScriptVersionMapper;
import com.scriptflow.framework.service.BaseService;
import com.scriptflow.framework.service.Converter;
import com.scriptflow.project.dto.ScriptVO;
import com.scriptflow.task.dto.TaskSubmitDTO;
import com.scriptflow.task.dto.TaskVO;
import com.scriptflow.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScriptService extends BaseService<Script, ScriptVO> {

    private final ScriptMapper scriptMapper;
    private final ScriptVersionMapper scriptVersionMapper;
    private final NovelChapterMapper chapterMapper;
    private final TaskService taskService;
    private final JsonUtils jsonUtils;
    private final FileStorageService fileStorageService;

    @Override
    protected BaseMapper<Script> getMapper() {
        return scriptMapper;
    }

    @Override
    protected Converter<Script, ScriptVO> getConverter() {
        return this::toVO;
    }

    public ScriptVO getByProjectId(Long projectId) {
        Script script = scriptMapper.selectOne(
                new LambdaQueryWrapper<Script>()
                        .eq(Script::getProjectId, projectId)
                        .orderByDesc(Script::getVersion)
                        .last("LIMIT 1"));
        if (script == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Script not found for this project");
        }
        return toVO(script);
    }

    @Transactional(rollbackFor = Exception.class)
    public ScriptVO submitGeneration(Long projectId, Long userId) {
        // Collect all chapter content
        List<NovelChapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<NovelChapter>()
                        .eq(NovelChapter::getProjectId, projectId)
                        .orderByAsc(NovelChapter::getChapterNo));

        // Build structured chapters JSON array
        List<Map<String, Object>> chaptersData = chapters.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("chapterNo", c.getChapterNo());
            map.put("title", c.getTitle() != null ? c.getTitle() : "");
            map.put("content", c.getContent() != null ? c.getContent() : "");
            map.put("wordCount", c.getWordCount() != null ? c.getWordCount() : 0);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> novelDocument = new HashMap<>();
        novelDocument.put("projectId", projectId);
        novelDocument.put("chapters", chaptersData);
        String novelJson = jsonUtils.toJson(novelDocument);

        // Upload to MinIO
        String minioKey = String.format("novel-content/%d/%d.json", projectId, System.currentTimeMillis());
        fileStorageService.uploadString(minioKey, novelJson);

        Script script = new Script();
        script.setProjectId(projectId);
        script.setVersion(1);
        script.setStatus(GlobalConstants.ScriptStatus.GENERATING);
        script.setWordCount(chaptersData.stream().mapToInt(c -> (int) c.get("wordCount")).sum());
        scriptMapper.insert(script);

        // Pass only reference key and metadata in params
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("scriptId", script.getId());
        paramsMap.put("projectId", projectId);
        paramsMap.put("minioKey", minioKey);
        paramsMap.put("chapterCount", chapters.size());
        paramsMap.put("chapterIds", chapters.stream().map(NovelChapter::getId).collect(Collectors.toList()));
        paramsMap.put("chapterNos", chapters.stream().map(NovelChapter::getChapterNo).collect(Collectors.toList()));
        String paramsJson = jsonUtils.toJson(paramsMap);

        TaskSubmitDTO taskDTO = new TaskSubmitDTO();
        taskDTO.setProjectId(projectId);
        taskDTO.setTaskType(GlobalConstants.TaskType.SCRIPT_GENERATE);
        taskDTO.setParams(paramsJson);
        TaskVO taskVO = taskService.submit(taskDTO, userId);

        ScriptVO result = toVO(script);
        result.setCurrentTaskId(taskVO.getId());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public ScriptVO updateContent(Long id, String yamlContent) {
        Script script = findByIdOrThrow(id);
        script.setYamlContent(yamlContent);
        script.setWordCount(yamlContent.length());
        script.setStatus(GlobalConstants.ScriptStatus.COMPLETED);
        scriptMapper.updateById(script);
        return toVO(script);
    }

    @Transactional(rollbackFor = Exception.class)
    public ScriptVO updateContentByProject(Long projectId, String yamlContent) {
        Script script = scriptMapper.selectOne(
                new LambdaQueryWrapper<Script>()
                        .eq(Script::getProjectId, projectId)
                        .orderByDesc(Script::getVersion)
                        .last("LIMIT 1"));
        if (script == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Script not found for project: " + projectId);
        }
        script.setYamlContent(yamlContent);
        script.setWordCount(yamlContent.length());
        script.setStatus(GlobalConstants.ScriptStatus.COMPLETED);
        scriptMapper.updateById(script);
        return toVO(script);
    }

    public List<ScriptVO> listVersions(Long scriptId) {
        return scriptVersionMapper.selectList(
                        new LambdaQueryWrapper<ScriptVersion>()
                                .eq(ScriptVersion::getScriptId, scriptId)
                                .orderByDesc(ScriptVersion::getVersionNo))
                .stream()
                .map(v -> {
                    ScriptVO vo = new ScriptVO();
                    vo.setId(v.getId());
                    vo.setVersion(v.getVersionNo());
                    vo.setYamlContent(v.getYamlContent());
                    vo.setCreateTime(v.getCreateTime());
                    return vo;
                })
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ScriptVO createVersion(Long scriptId, String yamlContent, String changeLog) {
        Script script = findByIdOrThrow(scriptId);

        ScriptVersion version = new ScriptVersion();
        version.setScriptId(scriptId);
        version.setVersionNo(script.getVersion() + 1);
        version.setYamlContent(yamlContent);
        version.setChangeLog(changeLog);
        scriptVersionMapper.insert(version);

        script.setVersion(version.getVersionNo());
        script.setYamlContent(yamlContent);
        script.setWordCount(yamlContent.length());
        scriptMapper.updateById(script);

        return toVO(script);
    }

    private ScriptVO toVO(Script script) {
        ScriptVO vo = new ScriptVO();
        vo.setId(script.getId());
        vo.setProjectId(script.getProjectId());
        vo.setVersion(script.getVersion());
        vo.setYamlContent(script.getYamlContent());
        vo.setWordCount(script.getWordCount());
        vo.setStatus(script.getStatus());
        vo.setErrorMsg(script.getErrorMsg());
        vo.setCreateTime(script.getCreateTime());
        vo.setUpdateTime(script.getUpdateTime());
        return vo;
    }
}
