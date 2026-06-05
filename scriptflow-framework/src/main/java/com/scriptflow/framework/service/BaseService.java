package com.scriptflow.framework.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.dal.entity.BaseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public abstract class BaseService<E extends BaseEntity, V> {

    protected abstract BaseMapper<E> getMapper();

    protected abstract Converter<E, V> getConverter();

    public V getById(Long id) {
        return getConverter().convert(findByIdOrThrow(id));
    }

    public PageUtils<V> page(int page, int pageSize, Wrapper<E> wrapper) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<E> mpPage =
                getMapper().selectPage(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize),
                        wrapper);
        List<V> records = mpPage.getRecords().stream()
                .map(getConverter()::convert)
                .toList();
        return PageUtils.of((int) mpPage.getCurrent(), (int) mpPage.getSize(),
                (int) mpPage.getTotal(), records);
    }

    public List<V> list(Wrapper<E> wrapper) {
        return getMapper().selectList(wrapper).stream()
                .map(getConverter()::convert)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        return getMapper().deleteById(id) > 0;
    }

    protected E findByIdOrThrow(Long id) {
        E entity = getMapper().selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return entity;
    }
}
