package io.typemorph.registry;

import io.typemorph.mapping.MorphMapping;

import java.util.Set;

public interface MorphRegistry {

    <S, T> void register(Class<S> sourceType, Class<T> targetType, MorphMapping<S, T> mapping);

    <S, T> MorphMapping<S, T> lookup(Class<S> sourceType, Class<T> targetType);

    /**
     * Returns the single registered target type for a source.
     * Throws MorphAmbiguousException if multiple targets exist.
     * Throws MorphNotFoundException if no mapping exists.
     */
    Class<?> resolveDefaultTarget(Class<?> sourceType);

    boolean hasMapping(Class<?> sourceType);

    boolean hasMapping(Class<?> sourceType, Class<?> targetType);

    Set<Class<?>> getTargetsFor(Class<?> sourceType);
}
