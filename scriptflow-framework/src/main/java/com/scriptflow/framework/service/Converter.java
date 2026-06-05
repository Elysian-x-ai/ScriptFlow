package com.scriptflow.framework.service;

@FunctionalInterface
public interface Converter<E, V> {
    V convert(E entity);
}
