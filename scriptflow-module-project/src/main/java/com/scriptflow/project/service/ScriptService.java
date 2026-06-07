package com.scriptflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.JsonUtils;
import com.scriptflow.storage.dto.MinioObjectItem;
import com.scriptflow.storage.service.FileStorageService;
import com.scriptflow.dal.entity.project.NovelChapter;
import com.scriptflow.dal.entity.project.Script;
import com.scriptflow.dal.entity.project.ScriptVersion;
import com.scriptflow.dal.mapper.project.NovelChapterMapper;
import com.scriptflow.dal.mapper.project.ScriptMapper;
import com.scriptflow.dal.mapper.project.ScriptVersionMapper;
import com.scriptflow.framework.service.BaseService;
import com.scriptflow.framework.service.Converter;
import com.scriptflow.project.dto.ChapterVO;
import com.scriptflow.project.dto.MinioYamlVO;
import com.scriptflow.project.dto.ScriptVO;
import com.scriptflow.task.dto.TaskSubmitDTO;
import com.scriptflow.task.dto.TaskVO;
import com.scriptflow.task.service.TaskService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptService extends BaseService<Script, ScriptVO> {

    private final ScriptMapper scriptMapper;
    private final ScriptVersionMapper scriptVersionMapper;
    private final NovelChapterMapper chapterMapper;
    private final TaskService taskService;
    private final JsonUtils jsonUtils;
    private final FileStorageService fileStorageService;
    private final NovelChapterService novelChapterService;

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
    public ScriptVO submitGeneration(Long projectId, Long userId, List<Long> chapterIds) {
        // Collect all chapter content (filter by chapterIds if provided)
        List<NovelChapter> allChapters = chapterMapper.selectList(
                new LambdaQueryWrapper<NovelChapter>()
                        .eq(NovelChapter::getProjectId, projectId)
                        .orderByAsc(NovelChapter::getChapterNo));

        List<NovelChapter> chapters;
        if (chapterIds != null && !chapterIds.isEmpty()) {
            chapters = allChapters.stream()
                    .filter(c -> chapterIds.contains(c.getId()))
                    .collect(Collectors.toList());
        } else {
            chapters = allChapters;
        }

        // Build structured chapters JSON array
        List<Map<String, Object>> chaptersData = chapters.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("chapterNo", c.getChapterNo());
            map.put("title", c.getTitle() != null ? c.getTitle() : "");
            map.put("content", c.getContent() != null ? c.getContent() : "");
            map.put("wordCount", c.getWordCount() != null ? c.getWordCount() : 0);
            return map;
        }).collect(Collectors.toList());

        // Check for existing script to preserve version history
        Script existingScript = scriptMapper.selectOne(
                new LambdaQueryWrapper<Script>()
                        .eq(Script::getProjectId, projectId)
                        .orderByDesc(Script::getVersion)
                        .last("LIMIT 1"));

        Map<String, Object> novelDocument = new HashMap<>();
        novelDocument.put("projectId", projectId);
        novelDocument.put("chapters", chaptersData);
        // Include previous script YAML for incremental generation context
        if (existingScript != null && existingScript.getYamlContent() != null) {
            novelDocument.put("previousYaml", existingScript.getYamlContent());
        }
        String novelJson = jsonUtils.toJson(novelDocument);

        // Upload to MinIO
        String minioKey = String.format("novel-content/%d/%d.json", projectId, System.currentTimeMillis());
        fileStorageService.uploadString(minioKey, novelJson);

        Script script;
        if (existingScript != null) {
            // Save current yamlContent as a version snapshot before overwriting
            if (existingScript.getYamlContent() != null) {
                ScriptVersion version = new ScriptVersion();
                version.setScriptId(existingScript.getId());
                version.setVersionNo(existingScript.getVersion());
                version.setYamlContent(existingScript.getYamlContent());
                version.setChangeLog("自动保存 - 重新生成前备份");
                version.setCreateBy(userId);
                scriptVersionMapper.insert(version);
            }
            // Reuse existing record with incremented version.
            // Keep yamlContent intact — if pipeline fails, old content remains visible.
            script = existingScript;
            script.setVersion(existingScript.getVersion() + 1);
            script.setStatus(GlobalConstants.ScriptStatus.GENERATING);
            script.setWordCount(chaptersData.stream().mapToInt(c -> (int) c.get("wordCount")).sum());
            script.setErrorMsg(null);
            script.setMinioKey(minioKey);
            scriptMapper.updateById(script);
        } else {
            script = new Script();
            script.setProjectId(projectId);
            script.setVersion(1);
            script.setStatus(GlobalConstants.ScriptStatus.GENERATING);
            script.setWordCount(chaptersData.stream().mapToInt(c -> (int) c.get("wordCount")).sum());
            script.setMinioKey(minioKey);
            scriptMapper.insert(script);
        }

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

    /**
     * Get chapters for display on the frontend (chapter selection dialog).
     * Always uses DB chapters (for reliable IDs), enriched with MinIO content when available.
     */
    public List<ChapterVO> getChaptersForDisplay(Long projectId) {
        // Base chapter list from DB (has IDs for filtering in submitGeneration)
        List<ChapterVO> chapters = novelChapterService.listByProjectId(projectId);

        // Enrich with full content from MinIO if available
        Script script = scriptMapper.selectOne(
                new LambdaQueryWrapper<Script>()
                        .eq(Script::getProjectId, projectId)
                        .orderByDesc(Script::getVersion)
                        .last("LIMIT 1"));

        if (script != null && script.getMinioKey() != null) {
            try {
                String json = fileStorageService.readString(script.getMinioKey());
                Map<String, Object> doc = jsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});
                List<Map<String, Object>> minioChapters = (List<Map<String, Object>>) doc.get("chapters");
                if (minioChapters != null) {
                    // Build chapterNo → content map from MinIO
                    Map<Integer, String> contentMap = new HashMap<>();
                    for (Map<String, Object> mc : minioChapters) {
                        if (mc.get("chapterNo") instanceof Number) {
                            int no = ((Number) mc.get("chapterNo")).intValue();
                            String content = mc.get("content") instanceof String ? (String) mc.get("content") : null;
                            if (content != null) {
                                contentMap.put(no, content);
                            }
                        }
                    }
                    // Fill full content from MinIO (DB listByProject only sets contentPreview)
                    for (ChapterVO vo : chapters) {
                        String fullContent = contentMap.get(vo.getChapterNo());
                        if (fullContent != null) {
                            vo.setContent(fullContent);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to enrich chapters from MinIO, using DB content: {}", e.getMessage());
            }
        }

        return chapters;
    }

    /**
     * Parse YAML object key to extract version number.
     * Key format: script-yaml/{projectId}/{scriptId}/v{version}_{timestamp}.yaml
     */
    private Integer parseVersionFromKey(String objectKey) {
        Pattern pattern = Pattern.compile("/v(\\d+)_\\d+\\.yaml$");
        Matcher matcher = pattern.matcher(objectKey);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parse YAML object key to extract script ID.
     */
    private Long parseScriptIdFromKey(String objectKey) {
        Pattern pattern = Pattern.compile("^script-yaml/\\d+/(\\d+)/");
        Matcher matcher = pattern.matcher(objectKey);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * List all YAML script files for a project from MinIO.
     */
    public List<MinioYamlVO> listYamlFromMinio(Long projectId) {
        String prefix = String.format("script-yaml/%d/", projectId);
        try {
            List<MinioObjectItem> items = fileStorageService.listObjects(prefix);
            List<MinioYamlVO> result = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (MinioObjectItem item : items) {
                MinioYamlVO vo = new MinioYamlVO();
                vo.setObjectKey(item.getObjectName());
                vo.setFileSize(item.getSize());
                vo.setProjectId(projectId);
                vo.setScriptId(parseScriptIdFromKey(item.getObjectName()));
                vo.setVersion(parseVersionFromKey(item.getObjectName()));
                if (item.getLastModified() != null) {
                    vo.setLastModified(sdf.format(item.getLastModified()));
                }
                result.add(vo);
            }

            // Sort by version descending (newest first)
            result.sort((a, b) -> {
                Integer va = a.getVersion() != null ? a.getVersion() : 0;
                Integer vb = b.getVersion() != null ? b.getVersion() : 0;
                return vb.compareTo(va);
            });

            return result;
        } catch (Exception e) {
            log.error("Failed to list YAML files from MinIO for project {}: {}", projectId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Read YAML content from MinIO by object key.
     */
    public String getYamlFromMinio(String objectKey) {
        return fileStorageService.readString(objectKey);
    }

    /**
     * Get chapter numbers from the last generation (from MinIO JSON).
     * Used by the frontend to pre-select only new chapters for regeneration.
     */
    public List<Integer> getLastGeneratedChapterNos(Long projectId) {
        Script script = scriptMapper.selectOne(
                new LambdaQueryWrapper<Script>()
                        .eq(Script::getProjectId, projectId)
                        .orderByDesc(Script::getVersion)
                        .last("LIMIT 1"));
        if (script == null || script.getMinioKey() == null) {
            return new ArrayList<>();
        }
        try {
            String json = fileStorageService.readString(script.getMinioKey());
            Map<String, Object> doc = jsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> chapters = (List<Map<String, Object>>) doc.get("chapters");
            if (chapters == null) return new ArrayList<>();
            return chapters.stream()
                    .map(c -> ((Number) c.get("chapterNo")).intValue())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to read last generated chapters from MinIO: {}", e.getMessage());
            return new ArrayList<>();
        }
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
        vo.setMinioKey(script.getMinioKey());
        vo.setCreateTime(script.getCreateTime());
        vo.setUpdateTime(script.getUpdateTime());
        return vo;
    }
}
