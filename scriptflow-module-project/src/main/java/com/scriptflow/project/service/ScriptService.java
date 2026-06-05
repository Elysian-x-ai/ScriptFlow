package com.scriptflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.dal.entity.project.Script;
import com.scriptflow.dal.entity.project.ScriptVersion;
import com.scriptflow.dal.mapper.project.ScriptMapper;
import com.scriptflow.dal.mapper.project.ScriptVersionMapper;
import com.scriptflow.project.dto.ScriptVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Script management service.
 */
@Service
@RequiredArgsConstructor
public class ScriptService {

    private final ScriptMapper scriptMapper;
    private final ScriptVersionMapper scriptVersionMapper;

    public ScriptVO getById(Long id) {
        Script script = scriptMapper.selectById(id);
        if (script == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Script not found");
        }
        return toVO(script);
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

    /**
     * Submit a script generation task.
     */
    @Transactional(rollbackFor = Exception.class)
    public ScriptVO submitGeneration(Long projectId, Long userId) {
        Script script = new Script();
        script.setProjectId(projectId);
        script.setVersion(1);
        script.setStatus(1); // generating
        script.setWordCount(0);
        scriptMapper.insert(script);
        return toVO(script);
    }

    /**
     * Update script YAML content (called after AI generation completes).
     */
    @Transactional(rollbackFor = Exception.class)
    public ScriptVO updateContent(Long id, String yamlContent) {
        Script script = scriptMapper.selectById(id);
        if (script == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Script not found");
        }
        script.setYamlContent(yamlContent);
        script.setWordCount(yamlContent.length());
        script.setStatus(2);
        scriptMapper.updateById(script);
        return toVO(script);
    }

    // ---- Version Management ----

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
        Script script = scriptMapper.selectById(scriptId);
        if (script == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Script not found");
        }

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
