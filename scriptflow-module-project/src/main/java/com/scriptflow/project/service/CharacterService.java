package com.scriptflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.common.util.BeanCopyUtils;
import com.scriptflow.common.util.StringUtils;
import com.scriptflow.dal.entity.project.CharacterEntity;
import com.scriptflow.dal.mapper.project.CharacterMapper;
import com.scriptflow.framework.service.BaseService;
import com.scriptflow.framework.service.Converter;
import com.scriptflow.project.dto.CharacterCreateDTO;
import com.scriptflow.project.dto.CharacterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterService extends BaseService<CharacterEntity, CharacterVO> {

    private final CharacterMapper characterMapper;

    @Override
    protected BaseMapper<CharacterEntity> getMapper() {
        return characterMapper;
    }

    @Override
    protected Converter<CharacterEntity, CharacterVO> getConverter() {
        return entity -> BeanCopyUtils.copy(entity, CharacterVO.class);
    }

    public List<CharacterVO> listByProjectId(Long projectId) {
        return list(new LambdaQueryWrapper<CharacterEntity>()
                .eq(CharacterEntity::getProjectId, projectId)
                .orderByAsc(CharacterEntity::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public CharacterVO create(CharacterCreateDTO dto) {
        CharacterEntity character = new CharacterEntity();
        character.setProjectId(dto.getProjectId());
        character.setName(dto.getName());
        character.setAlias(dto.getAlias());
        character.setGender(dto.getGender());
        character.setAge(dto.getAge());
        character.setPersonality(dto.getPersonality());
        character.setAppearance(dto.getAppearance());
        character.setBackground(dto.getBackground());
        character.setDescription(dto.getDescription());
        character.setRoleType(dto.getRoleType());
        characterMapper.insert(character);
        return getConverter().convert(character);
    }

    @Transactional(rollbackFor = Exception.class)
    public CharacterVO update(Long id, CharacterCreateDTO dto) {
        CharacterEntity character = findByIdOrThrow(id);
        if (StringUtils.isNotBlank(dto.getName())) character.setName(dto.getName());
        if (StringUtils.isNotBlank(dto.getAlias())) character.setAlias(dto.getAlias());
        if (StringUtils.isNotBlank(dto.getGender())) character.setGender(dto.getGender());
        if (StringUtils.isNotBlank(dto.getAge())) character.setAge(dto.getAge());
        if (StringUtils.isNotBlank(dto.getPersonality())) character.setPersonality(dto.getPersonality());
        if (StringUtils.isNotBlank(dto.getAppearance())) character.setAppearance(dto.getAppearance());
        if (StringUtils.isNotBlank(dto.getBackground())) character.setBackground(dto.getBackground());
        if (StringUtils.isNotBlank(dto.getDescription())) character.setDescription(dto.getDescription());
        if (StringUtils.isNotBlank(dto.getRoleType())) character.setRoleType(dto.getRoleType());
        characterMapper.updateById(character);
        return getConverter().convert(character);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        deleteById(id);
    }
}
