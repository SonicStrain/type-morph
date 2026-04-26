package io.typemorph.reflect;

import io.typemorph.exception.MorphInstantiationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

/**
 * Instantiates a POJO target using a constructor annotated with
 * {@link io.typemorph.annotation.TypeMorphConstructor}.
 *
 * <p>The annotated constructor is called with {@code null} for every parameter so
 * the object can be created without field values upfront; field values are then
 * set individually by {@link ReflectiveMorphMapping} using reflective field access.
 *
 * <p>This is useful when a POJO lacks a no-arg constructor but has a constructor
 * that accepts all or some fields — mark it with {@code @TypeMorphConstructor} and
 * TypeMorph will call it with nulls/defaults, then overwrite fields.
 *
 * @param <T> the target type
 */
final class AnnotatedConstructorInstantiationStrategy<T> implements InstantiationStrategy<T> {

    private final Constructor<T>  constructor;
    private final Object[]        defaultArgs;   // nulls / primitive defaults, built once

    AnnotatedConstructorInstantiationStrategy(Constructor<T> constructor) {
        this.constructor = constructor;
        this.defaultArgs = buildDefaultArgs(constructor.getParameters());
    }

    @Override
    public T newInstance(Object[] args) {
        // args is ignored — fields are set reflectively after construction
        try {
            return constructor.newInstance(defaultArgs);
        } catch (Exception e) {
            throw new MorphInstantiationException(
                    constructor.getDeclaringClass(),
                    "@TypeMorphConstructor constructor threw an exception", e);
        }
    }

    // -------------------------------------------------------------------------

    private static Object[] buildDefaultArgs(Parameter[] params) {
        Object[] defaults = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            defaults[i] = primitiveDefault(params[i].getType());
        }
        return defaults;
    }

    private static Object primitiveDefault(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class)    return '\0';
        if (type == byte.class)    return (byte)  0;
        if (type == short.class)   return (short) 0;
        if (type == int.class)     return 0;
        if (type == long.class)    return 0L;
        if (type == float.class)   return 0.0f;
        if (type == double.class)  return 0.0d;
        return null; // unreachable
    }
}
