package com.scriptflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.StringUtils;
import com.scriptflow.dal.entity.project.CharacterEntity;
import com.scriptflow.dal.mapper.project.CharacterMapper;
import com.scriptflow.project.dto.CharacterCreateDTO;
import com.scriptflow.project.dto.CharacterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Character management service.
 */
@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterMapper characterMapper;

    public List<CharacterVO> listByProjectId(Long projectId) {
        return characterMapper.selectList(
                        new LambdaQueryWrapper<CharacterEntity>()
                                .eq(CharacterEntity::getProjectId, projectId)
                                .orderByAsc(CharacterEntity::getCreateTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public CharacterVO getById(Long id) {
        CharacterEntity character = characterMapper.selectById(id);
        if (character == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Character not found");
        }
        return toVO(character);
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
        return toVO(character);
    }

    @Transactional(rollbackFor = Exception.class)
    public CharacterVO update(Long id, CharacterCreateDTO dto) {
        CharacterEntity character = characterMapper.selectById(id);
        if (character == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Character not found");
        }
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
        return toVO(character);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        characterMapper.deleteById(id);
    }

    private CharacterVO toVO(CharacterEntity character) {
        CharacterVO vo = new CharacterVO();
        vo.setId(character.getId());
        vo.setProjectId(character.getProjectId());
        vo.setName(character.getName());
        vo.setAlias(character.getAlias());
        vo.setGender(character.getGender());
        vo.setAge(character.getAge());
        vo.setPersonality(character.getPersonality());
        vo.setAppearance(character.getAppearance());
        vo.setBackground(character.getBackground());
        vo.setDescription(character.getDescription());
        vo.setRoleType(character.getRoleType());
        vo.setCreateTime(character.getCreateTime());
        return vo;
    }
}
