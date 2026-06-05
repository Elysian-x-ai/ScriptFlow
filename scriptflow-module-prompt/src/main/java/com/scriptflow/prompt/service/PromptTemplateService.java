package com.scriptflow.prompt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.common.util.StringUtils;
import com.scriptflow.dal.entity.prompt.PromptTemplate;
import com.scriptflow.dal.mapper.prompt.PromptTemplateMapper;
import com.scriptflow.prompt.dto.PromptTemplateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Prompt template management service.
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateMapper promptTemplateMapper;

    public PageUtils<PromptTemplate> page(int page, int pageSize, String category, String type) {
        LambdaQueryWrapper<PromptTemplate> wrapper = new LambdaQueryWrapper<PromptTemplate>()
                .eq(StringUtils.isNotBlank(category), PromptTemplate::getCategory, category)
                .eq(StringUtils.isNotBlank(type), PromptTemplate::getType, type)
                .orderByDesc(PromptTemplate::getUpdateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PromptTemplate> mpPage =
                promptTemplateMapper.selectPage(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize),
                        wrapper);

        return PageUtils.of((int) mpPage.getCurrent(), (int) mpPage.getSize(),
                (int) mpPage.getTotal(), mpPage.getRecords());
    }

    public List<PromptTemplate> listByCategory(String category) {
        return promptTemplateMapper.selectList(
                new LambdaQueryWrapper<PromptTemplate>()
                        .eq(PromptTemplate::getCategory, category)
                        .eq(PromptTemplate::getStatus, 1));
    }

    public PromptTemplate getById(Long id) {
        PromptTemplate template = promptTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Template not found");
        }
        return template;
    }

    public PromptTemplate getByCode(String code) {
        PromptTemplate template = promptTemplateMapper.selectOne(
                new LambdaQueryWrapper<PromptTemplate>().eq(PromptTemplate::getCode, code));
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Template not found: " + code);
        }
        return template;
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate create(PromptTemplateDTO dto) {
        PromptTemplate template = new PromptTemplate();
        template.setName(dto.getName());
        template.setCode(dto.getCode());
        template.setType(dto.getType());
        template.setCategory(dto.getCategory());
        template.setContent(dto.getContent());
        template.setVariables(dto.getVariables());
        template.setDescription(dto.getDescription());
        template.setVersion(1);
        template.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        promptTemplateMapper.insert(template);
        return template;
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate update(PromptTemplateDTO dto) {
        PromptTemplate template = promptTemplateMapper.selectById(dto.getId());
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Template not found");
        }
        if (StringUtils.isNotBlank(dto.getName())) template.setName(dto.getName());
        if (StringUtils.isNotBlank(dto.getContent())) template.setContent(dto.getContent());
        if (StringUtils.isNotBlank(dto.getVariables())) template.setVariables(dto.getVariables());
        if (dto.getStatus() != null) template.setStatus(dto.getStatus());
        template.setVersion(template.getVersion() + 1);
        promptTemplateMapper.updateById(template);
        return template;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        promptTemplateMapper.deleteById(id);
    }
}
