package com.scriptflow.dal.mapper.prompt;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.dal.entity.prompt.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {
}
