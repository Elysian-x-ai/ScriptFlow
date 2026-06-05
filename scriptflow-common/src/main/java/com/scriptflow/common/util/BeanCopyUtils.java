package com.scriptflow.common.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean copy utility wrapping Hutool BeanUtil.
 * Eliminates repetitive manual toVO() field-by-field copying.
 */
public final class BeanCopyUtils {

    private BeanCopyUtils() {}

    /**
     * Copy properties from source to a new instance of targetClass.
     */
    public static <T> T copy(Object source, Class<T> targetClass) {
        if (source == null) return null;
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtil.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy bean to " + targetClass.getSimpleName(), e);
        }
    }

    /**
     * Copy properties from source to existing target object.
     */
    public static void copy(Object source, Object target) {
        if (source == null || target == null) return;
        BeanUtil.copyProperties(source, target);
    }

    /**
     * Copy list of entities to list of VOs.
     */
    public static <T> List<T> copyList(Collection<?> sourceList, Class<T> targetClass) {
        if (CollUtil.isEmpty(sourceList)) return Collections.emptyList();
        return sourceList.stream()
                .map(source -> copy(source, targetClass))
                .collect(Collectors.toList());
    }
}
