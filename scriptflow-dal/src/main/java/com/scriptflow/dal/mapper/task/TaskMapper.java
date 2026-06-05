package com.scriptflow.dal.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.dal.entity.task.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
