package com.scriptflow.dal.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.dal.entity.task.TaskLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {
}
