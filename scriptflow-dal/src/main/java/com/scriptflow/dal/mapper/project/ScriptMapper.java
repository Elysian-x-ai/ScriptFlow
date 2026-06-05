package com.scriptflow.dal.mapper.project;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.dal.entity.project.Script;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScriptMapper extends BaseMapper<Script> {
}
